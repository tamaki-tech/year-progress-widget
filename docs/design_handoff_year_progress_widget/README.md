# Handoff: 今年あと何%（Year Progress Widget）— UI/UX 確定版

## 概要

ホーム画面ウィジェットで「今年が残り何%か」を表示する Android アプリのビジュアル仕様。対象は以下。

- ウィジェット 2 サイズ：**4×4** と **1×2**（1 列幅 × 2 行高の縦長）、それぞれダーク／ライト
- アプリ本体画面（MainActivity）
- アダプティブアイコン（前景・背景・モノクロ）

技術スタック・計算ロジック・更新方針はプロジェクトの `CLAUDE.md` に従う。本書はその「デザイン材料」セクションを確定値で置き換えるためのもの（末尾に CLAUDE.md 差し替え用の文面あり）。

## デザインファイルについて

同梱の `Year Progress.dc.html` は **HTML で作ったデザイン参照**であり、本番コードではない。見た目と挙動の意図を示すもの。実装は既存方針どおり **Jetpack Glance（ウィジェット）と Jetpack Compose / Material 3（アプリ本体）で作り直す**こと。HTML をそのまま移植しない。

ブラウザで開くとすべてのアートボードが表示される（ズーム可）。HTML 上は 1px = 1dp。

## 忠実度

**ハイファイ。** 色・文字サイズ・余白・角丸は確定値。ただし Glance の制約により以下は置き換える。

- フォント：HTML では Inter だが、実装は**システム既定フォント**（カスタムフォント不可のため）
- Android 12 以降は Dynamic Color に追従（下記「配色」）。表の固定色は Android 11 以下のフォールバックおよびプレビュー画像用

---

## 確定した仕様判断（CLAUDE.md の未決事項）

| 項目 | 決定 |
|---|---|
| 進捗バーで塗る量 | **残り**（「あと 13.4%」の数字とバーの長さが一致。年末に向けて減っていく） |
| 表記 | **「あと 13.4%」** |
| 1×1 円形表示 | 作らない（今回のサイズは 4×4 と 1×2） |
| バーの色 | 残り割合（表示値）が **30% より大きい → 緑**、**30% 以下 → 赤**。経過／残りどちらを塗る場合も判定は「残り割合」で行う |
| ウィジェットのサイズ | **4×4** と **1×2**（1×2 は縦長なので縦バー） |

判定は丸め後の表示値で行う：`displayPct > 30.0` なら緑、それ以外は赤（30.0% ちょうどは赤）。

---

## 画面ごとの仕様

### 1. ウィジェット 4×4

- **想定サイズ**：約 316×316dp（ランチャーにより変動。`SizeMode.Responsive` のブレークポイントは `DpSize(250.dp, 250.dp)`）
- **背景**：単色、角丸 `system_app_widget_background_radius`（参照値 24dp）
- **パディング**：全辺 24dp
- **レイアウト**（`Column`、上から順）：

| # | 要素 | 内容 | 文字 | 色（ダーク / ライト） | 間隔 |
|---|---|---|---|---|---|
| 1 | 年 | `2026` | 16sp Medium、等幅数字 | 補助テキスト | 先頭 |
| 2 | スペーサー | — | — | — | `defaultWeight()` で残り高さを埋める（以下を下寄せ） |
| 3 | ラベル | `あと` | 18sp Regular | 補助テキスト | — |
| 4 | 割合 | `13.4` ＋ `%`（同一行・ベースライン揃え） | 数字 88sp Medium、字間 -0.03em、等幅数字、行高 1.0 ／ `%` は 36sp Medium | 数字＝主テキスト／`%`＝補助テキスト | 上 4dp、数字と`%`の間 2dp |
| 5 | 進捗バー | 幅 100%、高さ 8dp、角丸 4dp | — | 塗り＝バー色（緑/赤）、トラック | 上 20dp |
| 6 | 目盛り | 左 `1月`、右 `12月`（`Row` 両端寄せ） | 12sp Regular | 目盛り文字 | 上 6dp |
| 7 | 残り日数 | `残り 49 日` | 18sp Regular、数字は等幅 | 主テキスト | 上 16dp |

- Glance 実装：バーは `LinearProgressIndicator(progress = remainingRatio.toFloat(), color = バー色, backgroundColor = トラック色)`、高さ 8dp。
- 目盛りの「1月／12月」はバーの左端・右端に対応する。

### 2. ウィジェット 1×2（縦長）

- **想定サイズ**：約 72×160dp（ブレークポイント `DpSize(57.dp, 110.dp)`）
- **背景・角丸**：4×4 と同じ
- **パディング**：上下 12dp、左右 8dp
- **レイアウト**（`Column`、水平中央寄せ、要素間 8dp）：

| # | 要素 | 内容 | 文字 | 色 |
|---|---|---|---|---|
| 1 | 年 | `2026` | 11sp Medium、等幅数字 | 補助テキスト |
| 2 | 縦バー | 幅 10dp、高さ＝残り全部（`defaultWeight()`）、角丸 5dp | — | トラック／塗り＝バー色（緑/赤） |
| 3 | 割合 | `13.4` ＋ `%`（ベースライン揃え） | 数字 18sp Medium、字間 -0.02em、等幅数字／`%` 11sp Regular | 数字＝主テキスト／`%`＝補助テキスト |

- 「あと」ラベルと残り日数は幅が足りないため**表示しない**。
- **縦バーの実装**：Glance の `LinearProgressIndicator` は横向きのみのため、`Box`（トラック色、`contentAlignment = Alignment.BottomCenter`）の中に `Box`（バー色、高さ = トラック高さ × remainingRatio）を置く。トラック高さは `LocalSize.current.height` からパディング・年・割合・間隔の高さを引いて算出する（目安：160 − 24 − 約14 − 約18 − 16 ≈ 88dp）。塗りは**下から**満ち、残りが減ると水位が下がる。
- 角丸：トラックは `cornerRadius(5.dp)`（Android 12+ のみ有効。11 以下は角丸なしで可）。

### 3. アプリ本体（MainActivity, Compose / Material 3）

- **背景**：アプリ背景色（ダーク #161826）。全画面 1 カラム、左揃え。
- **パディング**：左右 28dp、下 28dp。ステータスバー下から 28dp。
- **構成**（上から）：

| 要素 | 内容 | 文字 | 色 |
|---|---|---|---|
| アプリ名 | `今年あと何%` | 14sp Regular | #9397AB |
| （中央ブロック：残り高さの中で縦中央） | | | |
| 年 | `2026` | 20sp Medium | #9397AB |
| ラベル | `あと`（上 20dp） | 22sp | #B2B6CA |
| 割合 | `13.4` 104sp Medium 字間 -0.035em ＋ `%` 40sp Medium（上 6dp） | 等幅数字 | 数字 #E9E9ED／% #B2B6CA |
| 進捗バー | 高さ 8dp、角丸 4dp（上 24dp） | — | 塗り＝バー色（緑/赤）／トラック #3F424D |
| 目盛り | `1月` … `12月`（上 8dp） | 13sp | #75798C |
| 残り日数 | `残り 49 日`（上 20dp） | 20sp | #E9E9ED |
| （中央ブロックここまで） | | | |
| 説明文 | `ホーム画面に置くと、今年の残りがいつでも目に入ります。30分ごとに自動で更新され、通信や権限は使いません。` | 14sp、行高 1.7 | #B2B6CA |
| 追加ボタン（下 20dp 上） | `ホーム画面に追加` | 16sp Medium | — |
| 補足 | `ホーム画面の長押し →「ウィジェット」からも追加できます`（中央寄せ、上 12dp） | 12sp | #75798C |

- **追加ボタン**：`OutlinedButton`、全幅、高さ 52dp、角丸 26dp（ピル）、枠 1dp アクセント #9184D9、文字色 #D2CEFD、背景透明。押下時は枠と文字を #B5ABFC、背景にアクセント 12% のティント。
- タップで `AppWidgetManager.requestPinAppWidget(...)`（4×4 を既定）。`isRequestPinAppWidgetSupported == false` の場合はボタンを非表示にし、補足文だけ残す。

### 4. アダプティブアイコン

- **背景レイヤー**：単色 #161826
- **前景レイヤー**：108dp キャンバス中央、外径 約 50dp のリング（安全域 72dp の内側に収める）
  - トラック：#3F424D、線幅 約 6.3dp（キャンバス 108dp 基準）
  - 弧：#968AE0、同じ線幅、端は丸、**12 時から反時計回りに 3/4 周**（「残り 75%」の象徴。実データとは連動しない固定図形）
- **モノクロ（テーマアイコン）**：リングのみの 1 色レイヤー。トラック部分は不透明度 35%、弧は 100%。
- 書き出し：VectorDrawable（`ic_launcher_foreground.xml` / `ic_launcher_monochrome.xml`）。HTML 内の SVG（viewBox 56、r=22、stroke-width 7、dasharray 103.7/138.2）を元に変換する。

### 5. ウィジェットピッカー用プレビュー

- `previewLayout`：**ダーク 4×4** の固定レイアウト。値は `2026` / `あと 13.4%` / バー 13.4%（緑）/ `残り 49 日`。
- Android 11 以下用の `previewImage` も同じ見た目で書き出す。

---

## 配色

### Dynamic Color（Android 12+）での対応

| 役割 | Glance / M3 カラーロール |
|---|---|
| ウィジェット背景 | `GlanceTheme.colors.widgetBackground`（または surfaceContainer 相当） |
| 主テキスト | `onSurface` |
| 補助テキスト・目盛り | `onSurfaceVariant` |
| バー トラック | `secondaryContainer` |
| バー塗り | **固定の緑 / 赤**（下表。ダイナミックカラーには追従させない） |

### 固定色（フォールバック / プレビュー）

| 役割 | ダーク | ライト |
|---|---|---|
| ウィジェット背景 | #232532 | #F3F5FE |
| 主テキスト | #E9E9ED | #292B31 |
| 補助テキスト（年・あと・%） | #9397AB（年）/ #B2B6CA（あと・%） | #75798C（年）/ #595D6C（あと・%） |
| 目盛り文字 | #75798C | #75798C |
| バー トラック | #3F424D | #CFD3E5 |
| **バー（残り 30% 超）緑** | **#57BC80** — oklch(0.72 0.13 155) | **#14874E** — oklch(0.55 0.13 155) |
| **バー（残り 30% 以下）赤** | **#EA6A64** — oklch(0.68 0.16 25) | **#C13C3B** — oklch(0.55 0.17 25) |
| アプリ背景 | #161826 | — |
| アクセント（ボタン枠・アイコン） | #9184D9 | — |

Glance では `ColorProvider(day = ライト値, night = ダーク値)` で定義する。

---

## 挙動

- ウィジェットのタップ → `MainActivity` を開く（`actionStartActivity<MainActivity>()`）。
- 表示値・更新タイミング・丸め規則は `CLAUDE.md` の計算仕様どおり（実時間比、小数第1位**切り上げ**、0.0% は出さない、残り日数は今日を含む）。
- バー色の判定は表示値（切り上げ後）で行い、表示と色が食い違わないようにする。
- アニメーションなし（ウィジェットは静的。アプリ本体も初期リリースではなし）。
- リサイズ：`SizeMode.Responsive(setOf(DpSize(57.dp, 110.dp), DpSize(250.dp, 250.dp)))`。小さい方に近ければ 1×2 レイアウト、大きい方に近ければ 4×4 レイアウト。

## year_widget_info.xml の変更

現行の `minWidth="110dp"` では 1 列幅を置けないため、以下に変更する。

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

## strings.xml（日本語既定）

| key | 値 |
|---|---|
| `app_name` | 今年あと何% |
| `label_remaining_prefix` | あと |
| `percent_suffix` | % |
| `remaining_days` | 残り %1$d 日 |
| `month_start` | 1月 |
| `month_end` | 12月 |
| `main_description` | ホーム画面に置くと、今年の残りがいつでも目に入ります。30分ごとに自動で更新され、通信や権限は使いません。 |
| `add_to_home` | ホーム画面に追加 |
| `add_hint` | ホーム画面の長押し →「ウィジェット」からも追加できます |
| `widget_desc` | 今年の残りを % で表示します |

## 状態

- ウィジェット：保持する状態なし。描画時に `calcYearProgress()` を呼ぶだけ。
- アプリ本体：同じく描画時に計算。分単位の再計算は不要（画面表示時の値で十分）。

## ファイル

- `Year Progress.dc.html` — 全アートボード（01 ウィジェット、02 ホーム画面／アプリ本体、03 アイコン、04 数値）。ブラウザで直接開ける。
- `support.js`、`_ds/…/styles.css`、`_ds/…/_ds_bundle.js` — 上記 HTML を表示するための依存ファイル（実装では使わない）。

---

## CLAUDE.md 差し替え用（「デザイン材料」セクションを以下で置き換え）

```markdown
## デザイン（確定）

詳細は design_handoff_year_progress_widget/README.md。要点：

- サイズ：4×4（Responsive 250×250dp〜）と 1×2（57×110dp〜、縦長）
- 表記「あと 13.4%」、バーは「残り」を塗る
- バー色：表示値 > 30.0% → 緑（ダーク #57BC80 / ライト #14874E）、≤ 30.0% → 赤（#EA6A64 / #C13C3B）
- 4×4：年 16sp / あと 18sp / 割合 88sp + % 36sp / バー 8dp / 1月・12月 12sp / 残り日数 18sp、パディング 24dp、下寄せ
- 1×2：年 11sp / 縦バー 幅10dp（Box で下から塗る）/ 割合 18sp + % 11sp、パディング 12×8dp
- フォントはシステム既定、数字は等幅。角丸は system_app_widget_background_radius
- Android 12+ は Dynamic Color（バー色のみ固定）
```
