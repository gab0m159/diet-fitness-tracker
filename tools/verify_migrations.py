import argparse
import json
import os
import re
import sys

"""
校验 Migrations.kt 的建表 / 建索引语句与 Room 导出的 schema 是否一致。

## 为什么需要它

Room 打开数据库时会拿**迁移后的实际结构**和**代码期望的结构**逐列比对，不一致就抛
`IllegalStateException: Migration didn't properly handle ...`。这类错误只有真的跑一次
迁移才会暴露，手写 DDL 极易漏索引或写错类型。

这个脚本把「手工比对」变成一条命令：从 Migrations.kt 抽出 `execSQL` 的语句，
规范化后与 `<version>.json` 里的 `createSql` 逐条比对。

用法：
    python tools/verify_migrations.py            # 校验全部迁移
    python tools/verify_migrations.py --to 9     # 只校验目标版本为 9 的迁移
"""

SCHEMA_DIR = os.path.join(
    "app", "schemas", "com.example.diettracker.data.db.AppDatabase"
)
MIGRATIONS_FILE = os.path.join(
    "app", "src", "main", "java", "com", "example", "diettracker",
    "data", "db", "Migrations.kt"
)

# 建表 / 建索引语句的识别
RE_CREATE_TABLE = re.compile(
    r"^\s*CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?[`\[]?(\w+)[`\]]?",
    re.I,
)
RE_CREATE_INDEX = re.compile(
    r"^\s*CREATE\s+(?:UNIQUE\s+)?INDEX\s+(?:IF\s+NOT\s+EXISTS\s+)?[`\[]?(\w+)[`\]]?",
    re.I,
)


def normalize(sql: str, table_name: str = "") -> str:
    """
    规范化 DDL：把 schema 里的占位符换成真实表名、去反引号、折叠空白、去尾分号。

    Room 导出的 `createSql` 用的是 `${TABLE_NAME}` 占位符（因为同一个实体可能被
    多张表复用），比对时要替换成实际表名，否则永远对不上。
    """
    sql = sql.replace("${TABLE_NAME}", table_name)
    sql = sql.replace("`", "")
    sql = re.sub(r"\s+", " ", sql).strip()
    sql = sql.rstrip(";").strip()
    sql = re.sub(r"\s*IF\s+NOT\s+EXISTS\s*", " IF NOT EXISTS ", sql, flags=re.I)
    # 统一括号内外空格：schema 里是 `(a, b)`，手写时可能写成 `( a, b )`
    sql = re.sub(r"\(\s+", "(", sql)
    sql = re.sub(r"\s+\)", ")", sql)
    return sql.upper()


def load_schema(version: int):
    """读某版本的 schema，返回 {表名: {'create': sql, 'indices': {索引名: sql}}}。"""
    path = os.path.join(SCHEMA_DIR, f"{version}.json")
    if not os.path.exists(path):
        return None
    with open(path, encoding="utf-8") as fh:
        data = json.load(fh)

    out = {}
    for entity in data["database"]["entities"]:
        table = entity["tableName"]
        indices = {}
        for idx in entity.get("indices", []):
            m = RE_CREATE_INDEX.match(idx["createSql"])
            if m:
                indices[m.group(1).lower()] = idx["createSql"]
        out[table.lower()] = {"create": entity["createSql"], "indices": indices}
    return out


def extract_migrations():
    """
    从 Migrations.kt 抽出每个 Migration 的 execSQL 语句。

    返回 {目标版本: [sql, ...]}。做法是先按 `Migration(a, b)` 切块，再抓块内的
    三引号字符串和以 CREATE/INSERT/ALTER 开头的普通字符串。
    """
    with open(MIGRATIONS_FILE, encoding="utf-8") as fh:
        text = fh.read()

    result = {}
    parts = re.split(r"Migration\(\s*(\d+)\s*,\s*(\d+)\s*\)", text)
    # parts: [前言, from1, to1, body1, from2, to2, body2, ...]
    for i in range(1, len(parts) - 2, 3):
        to_version = int(parts[i + 1])
        body = parts[i + 2]

        # 三引号字符串里就是完整的 SQL（多行），先统一收集。
        sqls = [m.group(1) for m in re.finditer(r'"""\s*(.*?)\s*"""', body, re.S)]

        # 普通字符串里只挑以关键字开头的，并且**跳过三引号块内部**的片段，
        # 否则同一条语句会被算两次。
        body_without_triple = re.sub(r'"""\s*.*?\s*"""', "", body, flags=re.S)
        for m in re.finditer(r'"([^"]*)"', body_without_triple):
            frag = m.group(1)
            if re.match(r"\s*(CREATE|INSERT|ALTER|SELECT|UPDATE)", frag, re.I):
                sqls.append(frag)

        # 去掉纯 INSERT（迁移里给 seed_versions 写初始值，不参与结构比对）
        sqls = [s for s in sqls if not re.match(r"\s*INSERT", s, re.I)]
        result[to_version] = sqls
    return result


def main():
    parser = argparse.ArgumentParser(description="校验 Room 迁移与 schema 一致性")
    parser.add_argument("--to", type=int, help="只校验目标版本为该值的迁移")
    parser.add_argument("--verbose", "-v", action="store_true")
    args = parser.parse_args()

    if not os.path.exists(MIGRATIONS_FILE):
        print(f"[FAIL] 找不到 {MIGRATIONS_FILE}")
        return 1

    migrations = extract_migrations()
    if args.to is not None:
        migrations = {k: v for k, v in migrations.items() if k == args.to}

    if not migrations:
        print("[FAIL] 没解析到任何迁移——检查 Migrations.kt 的写法是否变了")
        return 1

    failures = []
    checked_tables = 0
    checked_indices = 0

    for to_version in sorted(migrations):
        schema = load_schema(to_version)
        if schema is None:
            print(f"[SKIP] v{to_version}: 没有 {to_version}.json")
            continue

        print(f"\n=== 迁移 -> v{to_version} ===")

        for sql in migrations[to_version]:
            flat = re.sub(r"\s+", " ", sql).strip()

            m = RE_CREATE_TABLE.match(flat)
            if m:
                table = m.group(1)
                key = table.lower()
                checked_tables += 1
                if key not in schema:
                    failures.append(f"v{to_version}: 迁移建了表 {table}，但 schema 里没有")
                    print(f"  [FAIL] 表 {table}: schema 里不存在")
                    continue
                expected = normalize(schema[key]["create"], table_name=table)
                actual = normalize(sql, table_name=table)
                if expected != actual:
                    failures.append(f"v{to_version}: 表 {table} 定义不一致")
                    print(f"  [FAIL] 表 {table}")
                    print(f"         期望: {expected}")
                    print(f"         实际: {actual}")
                else:
                    print(f"  [ OK ] 表 {table}")
                continue

            m = RE_CREATE_INDEX.match(flat)
            if m:
                index = m.group(1)
                checked_indices += 1
                found = None
                found_table = None
                for table_key, info in schema.items():
                    if index.lower() in info["indices"]:
                        found = info["indices"][index.lower()]
                        found_table = info
                        break
                if found is None:
                    failures.append(f"v{to_version}: 索引 {index} 在 schema 里没有")
                    print(f"  [FAIL] 索引 {index}: schema 里不存在")
                else:
                    # 索引的 createSql 也用 ${TABLE_NAME}，要换成它所属的表名
                    owner = None
                    for tname, info in schema.items():
                        if info is found_table:
                            owner = tname
                            break
                    if normalize(found, table_name=owner or "") != normalize(
                        sql, table_name=owner or ""
                    ):
                        failures.append(f"v{to_version}: 索引 {index} 定义不一致")
                        print(f"  [FAIL] 索引 {index}")
                        print(f"         期望: {normalize(found, owner or '')}")
                        print(f"         实际: {normalize(sql, owner or '')}")
                    else:
                        print(f"  [ OK ] 索引 {index}")

    print()
    if failures:
        print(f"[FAIL] {len(failures)} 处不一致——迁移会在真机上抛异常：")
        for f in failures:
            print(f"  - {f}")
        return 1

    print(f"[ OK ] 校验通过：{checked_tables} 张表 / {checked_indices} 个索引与 schema 一致")
    return 0


if __name__ == "__main__":
    sys.exit(main())
