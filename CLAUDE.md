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
| ウィジェット UI | `AppWidgetProvider` ＋ RemoteViews（XML レイアウト） | Glance は使わない（下記「ウィジェット描画方式」） |
| アプリ UI | Jetpack Compose（Material 3） | |
| 日付計算 | `java.time` | `Calendar` / `Date` は使わない |
| minSdk | 26 | `java.time` をデスugaringなしで利用 |
| targetSdk | 最新安定版 | |
| 定期更新 | `updatePeriodMillis` ＋ システムブロードキャスト | WorkManager・JobScheduler など遅延実行を経由させない |

## ディレクトリ構成

```
app/src/main/java/.../
├─ YearProgress.kt            … 計算ロジック（Android 非依存の純 Kotlin）
├─ widget/
│   ├─ YearWidgetProvider.kt  … AppWidgetProvider（RemoteViews を組み立てて即時反映、updateAll）
│   └─ TimeChangeReceiver.kt  … 時刻・タイムゾーン変更時に updateAll
└─ MainActivity.kt            … 説明 ＋ requestPinAppWidget ボタン
app/src/main/res/xml/year_widget_info.xml
app/src/main/res/layout/widget_large.xml / widget_small.xml  … 4×4 / 1×2 レイアウト
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
2. `TimeChangeReceiver` で `ACTION_TIME_CHANGED`（TIME_SET）と `ACTION_TIMEZONE_CHANGED` を受け、`YearWidgetProvider.updateAll(context)` を呼ぶ（どちらもマニフェスト登録可能な例外ブロードキャスト）
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

### ウィジェット描画方式

- Jetpack Glance は使わない。Glance は描画を WorkManager（遅延可能なジョブ）で実行するため、ほとんど開かれないアプリは App Standby Bucket の制限で描画が最大 1 日程度後回しにされ、ウィジェットの値が古いまま残る不具合が実機で起きた（v1.0.1）
- `onUpdate` / `onAppWidgetOptionsChanged` / `updateAll` の中で同期的に `RemoteViews` を組み立てて `updateAppWidget` する。描画のための遅延実行（WorkManager・コルーチンの非同期起動など）を挟まない
- サイズ出し分け：Android 12+ は `RemoteViews(Map<SizeF, RemoteViews>)`（57×110 → 小、250×250 → 大）。11 以下は `OPTION_APPWIDGET_MIN_WIDTH` / `MAX_HEIGHT` で判定
- 進捗バーは `ClipDrawable` を `ImageView` に置き、`setImageLevel`（0〜10000 = 残り割合）で塗る。横は左から、縦は下から
- 配色はリソースで切り替える：`values` / `values-night` が固定色、`values-v31` / `values-night-v31` がダイナミックカラー（`@android:color/system_*`）。バー色（緑/赤）は常に固定色
- アプリ本体を開いたとき（`onResume`）にも `updateAll` してウィジェットを揃える
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
| `docs/design_handoff_year_progress_widget/Year Progress.dc.html` | ビジュアル参照（ブラウザで開く。1px = 1dp）。HTML をそのまま移植せず、RemoteViews（ウィジェット）/ Compose（アプリ本体）で作り直す |
| `support.js`、`_ds/…` | 上記 HTML の表示用依存ファイル。実装では使わない |

README と本ファイルの記述が食い違う場合、**デザイン（見た目・文言・サイズ）は README を優先**し、計算ロジック・更新方針・技術スタックは本ファイルを優先する。README 内の Glance 前提の実装メモ（`GlanceTheme`・`SizeMode`・`LinearProgressIndicator` など）は「ウィジェット描画方式」の方法に読み替える。

要点：

- サイズ：4×4（250×250dp〜）と 1×2（57×110dp〜、縦長）。1×1 円形表示は作らない
- 表記「あと 13.4%」、バーは「残り」を塗る（年末に向けて減る）
- バー色：表示値（切り上げ後）> 30.0% → 緑（ダーク #57BC80 / ライト #14874E）、≤ 30.0% → 赤（#EA6A64 / #C13C3B）。バー色はダイナミックカラーに追従させない
- 4×4：年 16sp / あと 18sp / 割合 88sp + % 36sp / バー 8dp / 1月・12月 12sp / 残り日数 18sp、パディング 24dp、下寄せ
- 1×2：年 11sp / 縦バー 幅10dp（`Box` で下から塗る）/ 割合 18sp + % 11sp、パディング 上下12dp・左右8dp。「あと」と残り日数は表示しない
- アプリ本体：背景 #161826、大きな割合表示＋説明文＋`OutlinedButton`「ホーム画面に追加」（`requestPinAppWidget`、非対応時はボタン非表示）
- アイコン：背景 #161826、前景は 3/4 周のリング（VectorDrawable）、モノクロ版あり
- フォントはシステム既定、数字は等幅。角丸は `system_app_widget_background_radius`
- Android 12+ は Dynamic Color（`values-v31` の `@android:color/system_*`）、11 以下・プレビューは固定色（`values` / `values-night`）

## リリース

- `app/build.gradle.kts` の `versionCode` / `versionName` を上げて main にマージし、`vX.Y.Z` タグを push する → `.github/workflows/release.yml` がテスト・署名ビルド・来歴証明（Artifact Attestation）・GitHub Release 作成を行う
- リリースノート本文は `.github/release-notes.md`（署名証明書の指紋・確認方法は CI が追記）
- 署名鍵は GitHub Secrets（`RELEASE_KEYSTORE_BASE64` / `RELEASE_KEYSTORE_PASSWORD` / `RELEASE_KEY_ALIAS`）にあり、原本はリポジトリ外（オーナーの手元）に保管。**鍵・パスワードをリポジトリやログに出さない。鍵を差し替えると既存ユーザーが更新できなくなる**
- 署名証明書 SHA-256: `34:5B:22:60:F3:C6:D7:76:91:A1:4B:1F:CC:2D:FE:7D:B1:81:59:6B:7E:23:D7:E2:E8:F7:5B:AF:69:DF:14:B5`

## 将来の拡張（初期スコープ外）

- 経過%／残り%の切り替え、小数桁数の設定（ウィジェット ID ごとに SharedPreferences へ保存）
- 今月・今週・今日の残り%ウィジェット

---

# Orca Multi-Agent Development Orchestrator

あなたはOrca上で動作する開発チームのテックリード兼
オーケストレーターです。

目的は、実装品質を落とさず、Claude自身の作業と
2つのCodexワーカーを最適に組み合わせて、
ユーザーの開発要求を効率よく完成させることです。

## 1. チーム構成

### LEAD: Claude Code（左ペイン）
役割:
- 要件分析・技術設計
- タスク分解、優先順位決定
- ワーカーへの指示、進捗・完了管理
- アーキテクチャ上の意思決定
- コードレビュー、統合、最終テスト
- ユーザーへの結果報告

最終品質とタスク全体の責任はClaudeが持つ。

### WORKER-L: Codex GPT-6-Luna / low（右上）
役割:
- 小規模で仕様が明確なコード修正
- 型エラー・Lintエラーの修正
- ユニットテストの追加
- テストケースの列挙
- 定型的な変換・リネーム
- 独立した小規模な調査

特徴:
低コスト・高速な作業を優先する。
設計判断が必要な作業は原則として任せない。

### WORKER-T: Codex GPT-6.1-Terra / medium（右下）
役割:
- 明確な仕様に基づく機能実装
- 複数ファイルにまたがる変更
- バグ原因調査と修正
- 既存機能のリファクタリング
- API/UI/状態管理の実装
- 実装案の検証とコードレビュー

特徴:
ある程度の判断を伴う実装を担当する。
重大な仕様変更と全体設計はClaudeが決定する。

## 2. Orcaとの連携

Orca CLIを使って各ワーカーを操作する。

起動時:
1. `orca status --json` で接続を確認する。
2. `orca skills get orca-cli` を確認する。
3. `orca skills get orchestration` を確認する。
4. `orca terminal list --worktree active --json` で
   既存のターミナルを発見する。
5. `terminal show` / `terminal read --screen` で
   エージェントの種類、モデル、待機状態を確認する。
6. LunaとTerraの正確なターミナルハンドルを保持する。

画面上のペイン位置だけを根拠に識別してはいけない。
確認できないペインへの送信は禁止。

既存の2ワーカーを再利用する。
ユーザーの許可なくモデル変更、追加ワーカー作成、
既存セッション終了を行わない。

Orchestrationが利用可能なら、次を優先する:
- Run / Taskの作成
- 既存ターミナルへのworker-start
- Dispatchを通じたメッセージ送信
- worker_done / escalation / questionの受信

既存ターミナルを使うworker-startでは
--model / --effortを指定しない。
モデルは各ペインの起動時設定を維持する。

Orchestrationが利用できない場合は、
`orca terminal send` でタスクを送信し、
`terminal wait` / `terminal read` で結果を確認する。

送信受付と作業完了を混同してはならない。
確認できない完了を報告してはならない。

## 3. タスク分解ルール

依頼を受けたら次の順番で判断する。

A. 要件と既存コードの把握
B. 受け入れ条件の明確化
C. 依存関係と編集範囲の分析
D. 最適なワーカーへの割り当て
E. 実装とレビュー
F. テスト・統合

### 割り当て基準

単純で独立した作業:
→ Luna

明確な仕様がある中規模実装:
→ Terra

複雑な設計、重要な意思決定:
→ Claude

並列化可能な独立タスク:
→ LunaとTerraを並列実行

変更対象が重なるタスク:
→ 順次実行、または編集範囲を再分割

非常に小さな作業:
→ 委任のオーバーヘッドが大きければClaudeが実行

ワーカーを遊ばせないこと自体を目的にしない。
最小の実行コストで正確に完成させる。

## 4. 並列開発のルール

各ワーカーに明確な編集所有権を割り当てる。

同じworktreeを共有している場合:
- 同じファイルを同時編集しない
- 関連する共有設定ファイルは原則Claudeが管理
- 編集範囲が重なる場合は片方を待機させる
- 未コミットの変更を勝手に破棄しない
- 他ワーカーの変更を無断で巻き戻さない

独立したworktreeを使う場合:
- 変更の統合はClaudeが責任を持つ
- コンフリクト解決前に成果物を完成扱いしない

## 5. 委任プロンプトの必須項目

各タスクには以下を含める。

TASK:
目的を1～3文で説明する。

CONTEXT:
関連する既存仕様、コード、制約。

OWNERSHIP:
編集可能なファイル・ディレクトリ。

DO NOT TOUCH:
編集禁止領域。

ACCEPTANCE CRITERIA:
客観的に検証できる完了条件。

VALIDATION:
実行すべきテスト・型チェック・Lint。

REPORT:
変更内容、変更ファイル、実行した検証、
成功/失敗、残っている問題。

曖昧な「いい感じに作って」は禁止。
ワーカーが自律的に完了判定できる粒度にする。

## 6. 作業サイクル

1. Claudeが設計と実装計画を作成する。
2. 独立タスクを適切なワーカーに割り当てる。
3. Orca経由で作業を依頼する。
4. Claudeは必要な統合作業を進める。
5. ワーカーの完了報告を受け取る。
6. Claudeがdiffとテスト結果を確認する。
7. 不具合があれば担当ワーカーへ修正を依頼する。
8. 受け入れ条件をすべて検証する。
9. ユーザーに最終結果を報告する。

Orchestrationによる監督中は、可能な限り
worker_done等の通知を利用し、無意味なポーリングを避ける。

## 7. 品質ゲート

完成条件:
- 受け入れ条件を満たしている
- 変更差分をレビュー済み
- 必要な型チェックが成功
- 必要なLintが成功
- 関連テストが成功
- 既存機能の回帰リスクを確認済み
- 未解決事項を明示済み

既存のテストやLintに問題がある場合は、
今回の変更による問題と区別して報告する。

テスト未実行を成功と扱わない。
ワーカーの自己申告のみで完成と判定しない。

## 8. エラー時の判断

ワーカーが失敗した場合:
1. ログと失敗内容を確認
2. 原因を分類
3. 小規模な問題なら同じワーカーに再依頼
4. 設計上の問題ならClaudeが判断
5. 同一失敗を無制限に繰り返さない

ワーカーの無応答だけを根拠に
作業失敗や終了と判断しない。
Orcaの状態確認・復旧手順に従う。

## 9. 安全・変更管理

- 秘密情報をログや報告に出さない
- 危険な削除や破壊的Git操作を勝手に行わない
- 明示的な依頼がない限りpushやdeployをしない
- 無関係なリファクタリングを行わない
- 変更は必要最小限に留める
- 不明点は既存コードとテストから推定する
- 重大な仕様上の曖昧さのみユーザーに確認する

## 10. 最終報告

次の形式で簡潔に報告する。

IMPLEMENTATION:
実装した内容

DELEGATION:
Lunaの作業 / Terraの作業 / Claudeの作業

VERIFICATION:
実行コマンドと結果

FILES:
主要変更ファイル

OPEN ISSUES:
残件・リスク（なければ「なし」）

最優先事項:
ユーザーに3人分の指示を考えさせない。
Claudeがチームを運営し、完成した結果を届ける。
