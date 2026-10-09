"""校验 Room 迁移 MIGRATION_5_6（v5 -> v6）。

这个脚本做三件事，全部在内存 / 临时 SQLite 上完成，不依赖设备：

1. 按 v5 的实体定义手工建一个 v5 数据库，并往食物、饮食记录、身体数据、自建动作
   等表里塞入样本数据；
2. 从 `Migrations.kt` 里**原样抽取**迁移用的 SQL（而不是在这里抄一份），依次执行；
3. 断言：
   - 迁移前的饮食侧数据一条都没丢；
   - 重建后的 7 张训练表的列 / 类型 / NOT NULL / 主键 / 索引（含唯一性）/ 外键，
     与 Room 生成的 `app/schemas/.../6.json` 完全一致。

用法：python tools/verify_migration.py
"""

import json
import os
import re
import sqlite3
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MIGRATIONS_KT = os.path.join(
    ROOT, "app", "src", "main", "java", "com", "example", "diettracker",
    "data", "db", "Migrations.kt",
)
SCHEMA_JSON = os.path.join(
    ROOT, "app", "schemas", "com.example.diettracker.data.db.AppDatabase", "6.json",
)

# --------------------------------------------------------------------------- v5
# v5 的表结构（照实体定义写）。只有被迁移 DROP 掉的表需要完全一致，
# 但保留侧的表也照实写，这样第 3 步的数据断言才有意义。
V5_SCHEMA = [
    # ---- 饮食侧（迁移必须原样保留）----------------------------------------
    """CREATE TABLE IF NOT EXISTS `foods` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        `name` TEXT NOT NULL, `carbs_per_100g` REAL NOT NULL, `protein_per_100g` REAL NOT NULL,
        `fat_per_100g` REAL NOT NULL, `serving_size_grams` REAL NOT NULL, `note` TEXT NOT NULL,
        `category` TEXT NOT NULL, `brand_tag` TEXT NOT NULL, `nutrition_tags` TEXT NOT NULL,
        `daily_recommendation` TEXT NOT NULL, `data_source` TEXT NOT NULL,
        `credibility` TEXT NOT NULL, `has_variants` INTEGER NOT NULL,
        `source_note` TEXT NOT NULL, `created_at` INTEGER NOT NULL)""",
    "CREATE INDEX IF NOT EXISTS `index_foods_category` ON `foods` (`category`)",
    "CREATE INDEX IF NOT EXISTS `index_foods_brand_tag` ON `foods` (`brand_tag`)",
    """CREATE TABLE IF NOT EXISTS `food_variants` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        `food_id` INTEGER NOT NULL, `spec_name` TEXT NOT NULL, `position` INTEGER NOT NULL,
        `kilojoules` REAL NOT NULL, `kcal` REAL NOT NULL, `protein_g` REAL NOT NULL,
        `fat_g` REAL NOT NULL, `carbs_g` REAL NOT NULL, `sodium_mg` REAL NOT NULL,
        `calcium_mg` REAL NOT NULL,
        FOREIGN KEY(`food_id`) REFERENCES `foods`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )""",
    "CREATE INDEX IF NOT EXISTS `index_food_variants_food_id` ON `food_variants` (`food_id`)",
    """CREATE INDEX IF NOT EXISTS `index_food_variants_food_id_position`
        ON `food_variants` (`food_id`, `position`)""",
    """CREATE TABLE IF NOT EXISTS `diet_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        `date` TEXT NOT NULL, `food_id` INTEGER NOT NULL, `food_name` TEXT NOT NULL,
        `grams` REAL NOT NULL, `amount_mode` TEXT NOT NULL, `servings` REAL NOT NULL,
        `serving_size_grams` REAL NOT NULL, `carbs_per_100g` REAL NOT NULL,
        `protein_per_100g` REAL NOT NULL, `fat_per_100g` REAL NOT NULL,
        `meal_type` TEXT NOT NULL, `variant_spec` TEXT NOT NULL,
        `variant_kcal_per_serving` REAL NOT NULL, `variant_carbs_per_serving` REAL NOT NULL,
        `variant_protein_per_serving` REAL NOT NULL, `variant_fat_per_serving` REAL NOT NULL,
        `variant_sodium_per_serving` REAL NOT NULL, `variant_calcium_per_serving` REAL NOT NULL,
        `created_at` INTEGER NOT NULL,
        FOREIGN KEY(`food_id`) REFERENCES `foods`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )""",
    "CREATE INDEX IF NOT EXISTS `index_diet_entries_food_id` ON `diet_entries` (`food_id`)",
    "CREATE INDEX IF NOT EXISTS `index_diet_entries_date` ON `diet_entries` (`date`)",
    """CREATE TABLE IF NOT EXISTS `macro_goals` (`id` INTEGER NOT NULL,
        `carbs_grams` REAL NOT NULL, `protein_grams` REAL NOT NULL, `fat_grams` REAL NOT NULL,
        `updated_at` INTEGER NOT NULL, PRIMARY KEY(`id`))""",
    # ---- 身体数据 / 自建库（同样必须保留）---------------------------------
    """CREATE TABLE IF NOT EXISTS `user_profile` (`id` INTEGER NOT NULL, `height_cm` REAL NOT NULL,
        `weight_kg` REAL NOT NULL, `age` INTEGER NOT NULL, `sex` TEXT NOT NULL,
        `body_fat_percent` REAL, `activity_level` TEXT NOT NULL, `goal_mode` TEXT NOT NULL,
        `updated_at` INTEGER NOT NULL, PRIMARY KEY(`id`))""",
    """CREATE TABLE IF NOT EXISTS `custom_exercises` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        `name` TEXT NOT NULL, `body_part` TEXT NOT NULL, `primary_muscle` TEXT NOT NULL,
        `increment_kg` REAL NOT NULL, `stretch_names` TEXT NOT NULL, `cue` TEXT NOT NULL,
        `created_at` INTEGER NOT NULL)""",
    """CREATE TABLE IF NOT EXISTS `custom_stretches` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        `name` TEXT NOT NULL, `target_muscle` TEXT NOT NULL, `hold_seconds_min` INTEGER NOT NULL,
        `hold_seconds_max` INTEGER NOT NULL, `sets` INTEGER NOT NULL, `how_to` TEXT NOT NULL,
        `description` TEXT NOT NULL, `source` TEXT NOT NULL, `media_url` TEXT NOT NULL,
        `created_at` INTEGER NOT NULL)""",
    # ---- 训练侧（v5 形状，迁移会重建 / 删除）------------------------------
    """CREATE TABLE IF NOT EXISTS `training_splits` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        `name` TEXT NOT NULL, `body_part` TEXT NOT NULL, `position` INTEGER NOT NULL,
        `exercise_names` TEXT NOT NULL)""",
    """CREATE UNIQUE INDEX IF NOT EXISTS `index_training_splits_position`
        ON `training_splits` (`position`)""",
    """CREATE TABLE IF NOT EXISTS `training_plan` (`id` INTEGER NOT NULL, `sequence_index` INTEGER NOT NULL,
        `sequence_length` INTEGER NOT NULL, `last_advanced_date` TEXT,
        `weekly_training_days` TEXT NOT NULL, `postponed_split_day_index` INTEGER,
        `postponed_to_date` TEXT, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`id`))""",
    """CREATE TABLE IF NOT EXISTS `exercise_settings` (`exercise_name` TEXT NOT NULL,
        `increment_kg` REAL, `last_weight_kg` REAL, `updated_at` INTEGER NOT NULL,
        PRIMARY KEY(`exercise_name`))""",
    """CREATE TABLE IF NOT EXISTS `user_training_goal` (`id` INTEGER NOT NULL, `split_type` TEXT NOT NULL,
        `objective` TEXT NOT NULL, `schedule_mode` TEXT NOT NULL, `rest_after` INTEGER NOT NULL,
        `updated_at` INTEGER NOT NULL, PRIMARY KEY(`id`))""",
    """CREATE TABLE IF NOT EXISTS `workout_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        `date` TEXT NOT NULL, `split_day_index` INTEGER NOT NULL, `split_day_name` TEXT NOT NULL,
        `status` TEXT NOT NULL, `duration_minutes` INTEGER NOT NULL, `intensity` TEXT NOT NULL,
        `burned_kcal` REAL NOT NULL, `burn_overridden` INTEGER NOT NULL, `note` TEXT NOT NULL,
        `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL)""",
    "CREATE INDEX IF NOT EXISTS `index_workout_sessions_date` ON `workout_sessions` (`date`)",
    """CREATE INDEX IF NOT EXISTS `index_workout_sessions_date_status`
        ON `workout_sessions` (`date`, `status`)""",
    """CREATE TABLE IF NOT EXISTS `exercise_records` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        `session_id` INTEGER NOT NULL, `date` TEXT NOT NULL, `exercise_name` TEXT NOT NULL,
        `weight_kg` REAL NOT NULL, `sets` INTEGER NOT NULL, `reps_per_set` TEXT NOT NULL,
        `reps_min` INTEGER NOT NULL, `reps_max` INTEGER NOT NULL, `position` INTEGER NOT NULL,
        `created_at` INTEGER NOT NULL,
        FOREIGN KEY(`session_id`) REFERENCES `workout_sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )""",
    "CREATE INDEX IF NOT EXISTS `index_exercise_records_session_id` ON `exercise_records` (`session_id`)",
    """CREATE INDEX IF NOT EXISTS `index_exercise_records_exercise_name`
        ON `exercise_records` (`exercise_name`)""",
    """CREATE TABLE IF NOT EXISTS `exercise_status` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        `session_id` INTEGER NOT NULL, `date` TEXT NOT NULL, `exercise_name` TEXT NOT NULL,
        `outcome` TEXT NOT NULL, `weight_before_kg` REAL NOT NULL, `weight_after_kg` REAL NOT NULL,
        `reps_min` INTEGER NOT NULL, `reps_max` INTEGER NOT NULL, `sets` INTEGER NOT NULL,
        `position` INTEGER NOT NULL, `created_at` INTEGER NOT NULL)""",
    "CREATE INDEX IF NOT EXISTS `index_exercise_status_session_id` ON `exercise_status` (`session_id`)",
    """CREATE INDEX IF NOT EXISTS `index_exercise_status_exercise_name`
        ON `exercise_status` (`exercise_name`)""",
    "CREATE INDEX IF NOT EXISTS `index_exercise_status_date` ON `exercise_status` (`date`)",
]

# 迁移中会被重建的 7 张表，需要和 6.json 对结构。
TRAINING_TABLES = [
    "training_splits",
    "training_day_exercises",
    "day_schedule",
    "workout_sessions",
    "exercise_records",
    "exercise_status",
    "user_training_goal",
]

# 迁移必须保住数据的表。
PRESERVED = {
    "foods": "INSERT INTO `foods` (`name`,`carbs_per_100g`,`protein_per_100g`,`fat_per_100g`,"
             "`serving_size_grams`,`note`,`category`,`brand_tag`,`nutrition_tags`,"
             "`daily_recommendation`,`data_source`,`credibility`,`has_variants`,"
             "`source_note`,`created_at`) "
             "VALUES ('我自建的鸡胸肉',0,31,3.6,100,'用户在应用里建的食物','USER','','优质蛋白质',"
             "'每天约 100-150g','用户自建','OFFICIAL',0,'',1)",
    "food_variants": "INSERT INTO `food_variants` (`food_id`,`spec_name`,`position`,`kilojoules`,"
                     "`kcal`,`protein_g`,`fat_g`,`carbs_g`,`sodium_mg`,`calcium_mg`) "
                     "VALUES (1,'中',0,1000,240,10,8,30,500,80)",
    "macro_goals": "INSERT INTO `macro_goals` (`id`,`carbs_grams`,`protein_grams`,`fat_grams`,"
                   "`updated_at`) VALUES (1,250,125,55,1)",
    "user_profile": "INSERT INTO `user_profile` (`id`,`height_cm`,`weight_kg`,`age`,`sex`,"
                    "`body_fat_percent`,`activity_level`,`goal_mode`,`updated_at`) "
                    "VALUES (1,175,70,25,'MALE',18.5,'MODERATE','MAINTAIN',1)",
    "custom_exercises": "INSERT INTO `custom_exercises` (`name`,`body_part`,`primary_muscle`,"
                        "`increment_kg`,`stretch_names`,`cue`,`created_at`) "
                        "VALUES ('我的自创动作','CHEST','胸大肌',2.5,'','',1)",
    "custom_stretches": "INSERT INTO `custom_stretches` (`name`,`target_muscle`,`hold_seconds_min`,"
                        "`hold_seconds_max`,`sets`,`how_to`,`description`,`source`,`media_url`,"
                        "`created_at`) VALUES ('我的拉伸','胸大肌',20,30,3,'靠墙','','用户自建','',1)",
}
PRESERVED_DIET_ENTRY = (
    "INSERT INTO `diet_entries` (`date`,`food_id`,`food_name`,`grams`,`amount_mode`,`servings`,"
    "`serving_size_grams`,`carbs_per_100g`,`protein_per_100g`,`fat_per_100g`,`meal_type`,"
    "`variant_spec`,`variant_kcal_per_serving`,`variant_carbs_per_serving`,"
    "`variant_protein_per_serving`,`variant_fat_per_serving`,`variant_sodium_per_serving`,"
    "`variant_calcium_per_serving`,`created_at`) "
    "VALUES ('2026-09-27',1,'我自建的鸡胸肉',150,'GRAM',1,100,0,31,3.6,'LUNCH','',0,0,0,0,0,0,1)"
)


def extract_migration_sql(path):
    """从 Migrations.kt 里抽出 execSQL 用的 SQL 语句（按文件里的顺序）。"""
    text = open(path, encoding="utf-8").read()
    literals = re.findall(r'"((?:[^"\\]|\\.)*)"', text)
    stream = " ".join(literals).replace("System.currentTimeMillis()", "0")
    chunks = re.split(
        r"(?=(?:CREATE TABLE|CREATE INDEX|CREATE UNIQUE INDEX|DROP TABLE|INSERT OR REPLACE))",
        stream,
    )
    statements = [c.strip() for c in chunks if c.strip()]
    keywords = ("CREATE TABLE", "CREATE INDEX", "CREATE UNIQUE INDEX", "DROP TABLE",
                "INSERT OR REPLACE")
    return [s for s in statements if s.startswith(keywords)]


def column_map(conn, table):
    rows = conn.execute(f"PRAGMA table_info(`{table}`)").fetchall()
    return {
        r[1]: {"type": (r[2] or "").upper(), "notnull": int(r[3]), "pk": int(r[5])}
        for r in rows
    }


def index_map(conn, table):
    result = {}
    for row in conn.execute(f"PRAGMA index_list(`{table}`)").fetchall():
        name = row[1]
        unique = int(row[2])
        cols = [c[2] for c in conn.execute(f"PRAGMA index_info(`{name}`)").fetchall()]
        result[name] = {"unique": unique, "columns": cols}
    return result


def foreign_keys(conn, table):
    rows = conn.execute(f"PRAGMA foreign_key_list(`{table}`)").fetchall()
    return sorted(
        (r[2], r[3], r[4], r[5], r[6]) for r in rows  # table, from, to, on_update, on_delete
    )


def main():
    if not os.path.exists(SCHEMA_JSON):
        print("找不到 6.json，先跑一次 :app:kspDebugKotlin")
        return 1

    schema = json.load(open(SCHEMA_JSON, encoding="utf-8"))
    entities = {e["tableName"]: e for e in schema["database"]["entities"]}

    statements = extract_migration_sql(MIGRATIONS_KT)
    if not statements:
        print("没有从 Migrations.kt 抽到任何 SQL，检查抽取逻辑")
        return 1

    conn = sqlite3.connect(":memory:")
    conn.execute("PRAGMA foreign_keys = OFF")
    for ddl in V5_SCHEMA:
        conn.execute(ddl)
    conn.execute("PRAGMA user_version = 5")

    for stmt in list(PRESERVED.values()) + [PRESERVED_DIET_ENTRY]:
        conn.execute(stmt)
    # v5 训练数据：迁移后应当消失
    conn.execute(
        "INSERT INTO `training_splits` (`name`,`body_part`,`position`,`exercise_names`) "
        "VALUES ('胸 + 三头','CHEST',0,'杠铃卧推,上斜卧推')"
    )
    conn.execute(
        "INSERT INTO `exercise_settings` (`exercise_name`,`increment_kg`,`last_weight_kg`,"
        "`updated_at`) VALUES ('杠铃卧推',2.5,60,1)"
    )
    conn.execute(
        "INSERT INTO `training_plan` (`id`,`sequence_index`,`sequence_length`,"
        "`weekly_training_days`,`updated_at`) VALUES (1,0,4,'1,3,5',1)"
    )
    conn.execute(
        "INSERT INTO `user_training_goal` (`id`,`split_type`,`objective`,`schedule_mode`,"
        "`rest_after`,`updated_at`) VALUES (1,'THREE','HYPERTROPHY','SEQUENCE',3,1)"
    )
    conn.commit()

    before = {
        table: conn.execute(f"SELECT COUNT(*) FROM `{table}`").fetchone()[0]
        for table in list(PRESERVED.keys()) + ["diet_entries"]
    }

    # ------------------------------------------------------------ 跑迁移
    failures = []
    for stmt in statements:
        try:
            conn.execute(stmt)
        except sqlite3.Error as exc:
            failures.append(f"SQL 执行失败: {exc}\n  {stmt[:160]}")
    conn.commit()

    if failures:
        print("迁移 SQL 执行失败：")
        for f in failures:
            print(" -", f)
        return 1

    # ------------------------------------------------- 1. 保留表态检查
    after = {
        table: conn.execute(f"SELECT COUNT(*) FROM `{table}`").fetchone()[0]
        for table in list(PRESERVED.keys()) + ["diet_entries"]
    }
    print("== 迁移前 / 后行数 ==")
    ok_preserved = True
    for table, count in before.items():
        now = after[table]
        mark = "OK " if now == count else "丢失"
        if now != count:
            ok_preserved = False
        print(f"  [{mark}] {table}: {count} -> {now}")

    for gone in ("training_plan", "exercise_settings"):
        exists = conn.execute(
            "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name=?", (gone,)
        ).fetchone()[0]
        print(f"  [{'OK ' if exists == 0 else '残留'}] 旧表已删除: {gone}")

    print("\n== 自建食物内容抽查 ==")
    row = conn.execute(
        "SELECT name, category, nutrition_tags FROM `foods` WHERE name='我自建的鸡胸肉'"
    ).fetchone()
    print("  foods 行:", row)
    entry = conn.execute(
        "SELECT food_name, grams, meal_type FROM `diet_entries` WHERE date='2026-09-27'"
    ).fetchone()
    print("  diet_entries 行:", entry)

    # ------------------------------------- 2. 与 Room 6.json 逐表对结构
    print("\n== 表结构与 6.json 对比 ==")
    ok_schema = True
    for table in TRAINING_TABLES:
        entity = entities.get(table)
        if entity is None:
            print(f"  [FAIL] 6.json 里没有 {table}")
            ok_schema = False
            continue

        expected_cols = {}
        pk_columns = list(entity.get("primaryKey", {}).get("columnNames", []))
        for f in entity["fields"]:
            name = f["columnName"]
            expected_cols[name] = {
                "type": f["affinity"].upper(),
                "notnull": 1 if f["notNull"] else 0,
                "pk": (pk_columns.index(name) + 1) if name in pk_columns else 0,
            }
        actual_cols = column_map(conn, table)
        if expected_cols != actual_cols:
            ok_schema = False
            print(f"  [FAIL] {table} 列不一致")
            for name in sorted(set(expected_cols) | set(actual_cols)):
                e, a = expected_cols.get(name), actual_cols.get(name)
                if e != a:
                    print(f"        {name}: 期望 {e} 实际 {a}")
        else:
            print(f"  [OK ] {table} 列一致（{len(actual_cols)} 列）")

        # 索引
        expected_idx = {
            i["name"]: {"unique": 1 if i["unique"] else 0, "columns": list(i["columnNames"])}
            for i in entity.get("indices", [])
        }
        actual_idx = index_map(conn, table)
        actual_idx = {k: v for k, v in actual_idx.items() if not k.startswith("sqlite_autoindex")}
        if expected_idx != actual_idx:
            ok_schema = False
            print(f"  [FAIL] {table} 索引不一致")
            for name in sorted(set(expected_idx) | set(actual_idx)):
                e, a = expected_idx.get(name), actual_idx.get(name)
                if e != a:
                    print(f"        {name}: 期望 {e} 实际 {a}")
        else:
            print(f"  [OK ] {table} 索引一致（{len(actual_idx)} 个）")

        # 外键：Room 的 `table` 是父表（被引用方），`columns` 是子表列。
        expected_fk = sorted(
            (fk["table"], fk["columns"][0], fk["referencedColumns"][0],
             fk.get("onUpdate") or "NO ACTION", fk["onDelete"] or "NO ACTION")
            for fk in entity.get("foreignKeys", [])
        )
        actual_fk = foreign_keys(conn, table)
        if expected_fk != actual_fk:
            ok_schema = False
            print(f"  [FAIL] {table} 外键不一致：期望 {expected_fk} 实际 {actual_fk}")
        else:
            if expected_fk:
                print(f"  [OK ] {table} 外键一致（{len(expected_fk)} 个）")

    print("\n== 结论 ==")
    print("  饮食 / 身体数据 / 自建库保留：", "通过" if ok_preserved else "不通过")
    print("  训练表结构与 Room 期望一致：", "通过" if ok_schema else "不通过")
    return 0 if (ok_preserved and ok_schema) else 1


if __name__ == "__main__":
    sys.exit(main())
