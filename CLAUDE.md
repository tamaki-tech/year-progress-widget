# CLAUDE.md — 今年あと何%（Year Progress Widget）

このファイルは、本プロジェクトで作業する Claude（および開発者）向けの指針です。実装前に必ず読むこと。

## プロジェクト概要

ホーム画面ウィジェットで「今年が残り何%か」を表示するシンプルな Android アプリ。

- 主機能：ウィジェットに「残り◯%」「進捗バー」「残り日数」を表示
- アプリ本体：説明と「ウィジェットを追加」ボタンのみの最小画面（インストール後の初回起動で stopped state を解除する役割も兼ねる）
- 通信・権限：不要。完全オフライン
- 設定画面：初期リリースでは作らない

## 技術スタック

| 項目 | 選定 | 補足 |
|---|---|---|
| 言語 | Kotlin | |
| ウィジェット UI | Jetpack Glance | RemoteViews を直接書かない |
| アプリ UI | Jetpack Compose（Material 3） | |
| 日付計算 | `java.time` | `Calendar` / `Date` は使わない |
| minSdk | 26 | `java.time` をデスugaringなしで利用 |
| targetSdk | 最新安定版 | |
| 定期更新 | `updatePeriodMillis` ＋ システムブロードキャスト | WorkManager は導入しない |

## ディレクトリ構成

```
app/src/main/java/.../
├─ YearProgress.kt            … 計算ロジック（Android 非依存の純 Kotlin）
├─ widget/
│   ├─ YearWidget.kt          … GlanceAppWidget（表示）
│   ├─ YearWidgetReceiver.kt  … GlanceAppWidgetReceiver
│   └─ TimeChangeReceiver.kt  … 時刻・タイムゾーン変更時に updateAll
└─ MainActivity.kt            … 説明 ＋ requestPinAppWidget ボタン
app/src/main/res/xml/year_widget_info.xml
app/src/test/.../YearProgressTest.kt
docs/design_handoff_year_progress_widget/  … UI/UX 設計資料（「デザイン（確定）」参照）
```

## 計算ロジック（仕様）

```kotlin
data class YearProgress(val year: Int, val remainingRatio: Double, val remainingDays: Long)

fun calcYearProgress(clock: Clock = Clock.systemDefaultZone()): YearProgress {
    val zone = clock.zone
    val now = ZonedDateTime.now(clock)
    val start = LocalDate.of(now.year, 1, 1).atStartOfDay(zone)
    val end = start.plusYears(1)
    val total = Duration.between(start, end).toMillis().toDouble()
    val remaining = Duration.between(now, end).toMillis().toDouble()
    val days = ChronoUnit.DAYS.between(now.toLocalDate(), end.toLocalDate())
    return YearProgress(now.year, remaining / total, days)
}
```

ルール：

- 割合は「開始〜終了の実時間」で割る。うるう年・夏時間はこれで自動的に正しくなる。日数 ÷ 365 のような固定値計算はしない
- `Clock` を必ず引数で受け取る（テスト容易性のため）。関数内で `now()` を直接呼ばない
- 表示は小数第1位。**切り上げ**で丸め、年末でも 0.0% と表示しない（1/1 0:00 は 100.0%）
- 残り日数は「今日を含む残り日数」（例：11/13 → 49日、12/31 → 1日）

## 更新方針

1時間あたりの変化は約 0.011%（小数第1位の表示が変わるのは約 8.8 時間ごと）なので、高頻度更新は不要。

1. `updatePeriodMillis = 1800000`（30分。OS の最小値）
2. `TimeChangeReceiver` で `ACTION_TIME_CHANGED`（TIME_SET）と `ACTION_TIMEZONE_CHANGED` を受け、`YearWidget().updateAll(context)` を呼ぶ（どちらもマニフェスト登録可能な例外ブロードキャスト）
3. 年またぎは最大30分の遅れを許容する。厳密にする場合のみ `AlarmManager.setWindow` で翌 1/1 0:00 に予約（`SCHEDULE_EXACT_ALARM` 権限は使わない）

`year_widget_info.xml`：

```xml
<appwidget-provider
    android:minWidth="40dp" android:minHeight="110dp"
    android:minResizeWidth="40dp" android:minResizeHeight="110dp"
    android:targetCellWidth="4" android:targetCellHeight="4"
    android:resizeMode="horizontal|vertical"
    android:updatePeriodMillis="1800000"
    android:widgetCategory="home_screen"
    android:previewLayout="@layout/widget_preview"
    android:description="@string/widget_desc" />
```

## 実装上の注意

- Glance の `CircularProgressIndicator` は進捗値を指定できない（indeterminate のみ）。円形表示が必要なら Bitmap 描画か画像リソースで対応し、まずは `LinearProgressIndicator` を使う
- `SizeMode.Responsive` を使い、サイズごとにレイアウトを出し分ける（下記「デザイン（確定）」参照）
- ウィジェットのタップで `MainActivity` を開く
- 文字列は `strings.xml` に置く（日本語を既定にし、英語は後で追加）。キーと値はデザイン資料 README の「strings.xml」に従う

## テスト

`YearProgressTest` で固定の `Clock` を使い、以下を検証する。

- 1/1 00:00 → 100.0%
- 12/31 23:59 → 0.1%（切り上げ）
- 2026/11/13 00:00 → 約 13.4%・残り 49 日
- うるう年（2028）の 2/28〜3/1 前後、総日数 366 日
- 夏時間のある地域（例：America/New_York）の切り替え日

手動確認：タイムゾーン変更で即時更新されること、リサイズでレイアウトが切り替わること。

---

## デザイン（確定）

UI/UX の設計資料は **`docs/design_handoff_year_progress_widget/`** にある。ウィジェット・アプリ本体・アイコン・プレビュー・strings.xml を実装／変更するときは、必ず先に参照すること。

| ファイル | 内容 |
|---|---|
| `docs/design_handoff_year_progress_widget/README.md` | **確定仕様（正）**。各サイズのレイアウト・文字サイズ・余白・配色（Dynamic Color 対応表／固定色）・挙動・`year_widget_info.xml`・`strings.xml` の値 |
| `docs/design_handoff_year_progress_widget/Year Progress.dc.html` | ビジュアル参照（ブラウザで開く。1px = 1dp）。HTML をそのまま移植せず、Glance / Compose で作り直す |
| `support.js`、`_ds/…` | 上記 HTML の表示用依存ファイル。実装では使わない |

README と本ファイルの記述が食い違う場合、**デザイン（見た目・文言・サイズ）は README を優先**し、計算ロジック・更新方針・技術スタックは本ファイルを優先する。

要点：

- サイズ：4×4（Responsive `DpSize(250.dp, 250.dp)`〜）と 1×2（`DpSize(57.dp, 110.dp)`〜、縦長）。1×1 円形表示は作らない
- 表記「あと 13.4%」、バーは「残り」を塗る（年末に向けて減る）
- バー色：表示値（切り上げ後）> 30.0% → 緑（ダーク #57BC80 / ライト #14874E）、≤ 30.0% → 赤（#EA6A64 / #C13C3B）。バー色はダイナミックカラーに追従させない
- 4×4：年 16sp / あと 18sp / 割合 88sp + % 36sp / バー 8dp / 1月・12月 12sp / 残り日数 18sp、パディング 24dp、下寄せ
- 1×2：年 11sp / 縦バー 幅10dp（`Box` で下から塗る）/ 割合 18sp + % 11sp、パディング 上下12dp・左右8dp。「あと」と残り日数は表示しない
- アプリ本体：背景 #161826、大きな割合表示＋説明文＋`OutlinedButton`「ホーム画面に追加」（`requestPinAppWidget`、非対応時はボタン非表示）
- アイコン：背景 #161826、前景は 3/4 周のリング（VectorDrawable）、モノクロ版あり
- フォントはシステム既定、数字は等幅。角丸は `system_app_widget_background_radius`
- Android 12+ は Dynamic Color（`GlanceTheme`）、11 以下・プレビューは固定色（`ColorProvider(day, night)`）

## 将来の拡張（初期スコープ外）

- 経過%／残り%の切り替え、小数桁数の設定（`PreferencesGlanceStateDefinition` でウィジェットごとに保存）
- 今月・今週・今日の残り%ウィジェット
