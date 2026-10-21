<!-- GitHub publication copy. Machine-specific absolute paths were normalized. -->

> 发布版说明：本文由本地设计/复现手册整理而来，已清理个人绝对路径与机器相关细节；历史构建、测试和真机验证结论按当时记录保留。

# BiliPai 新版本复现总手册（四份文档合并版）

> 合并自：`BiliPai_本轮修改汇总.md`（A系列） + `BiliPai-Custom-Modifications-Summary.md`（UI/竖屏批） + `bilipai-perf-round1-summary.md`（性能批） + `BiliPai_Round2_Summary.md`（Plan 复盘）
> 用途：上游 BiliPai 发布新版本后，按本手册操作即可完全复现当前自定义行为。

---

## 0. 当前状态快照（2026-08-28 更新：工作区干净，全部个性化定制已入 git 历史，可随时 cherry-pick 或按手册重做）

- **仓库位置（新）**：`%USERPROFILE%\Desktop\Bili\BiliPai`（旧路径 `%USERPROFILE%\Documents\Default Project\BiliPai` 已废弃；`BiliPai_Backup`、`PiliPlus` 同在桌面 Bili 目录）
- **HEAD**：`795897c`（08-28，UI 细节一致性整改批 S 轮；**本地 main 领先上游 origin 40 个提交**，push 待凭证恢复）
- **工作区未提交**：无（`git status` 干净）
- **Git 提交时间线**（旧→新）：
  A 系列 → 批F/UI/竖屏批（20 个提交） → 性能批（2 个提交） → 本轮基线 2 个提交：`1c8d32c`（UI调整：首页顶栏步长/底纹 13dp/分类页加载键）→ `e23a0b0`（播放器：竖屏详情页 9:16 竖条等宽顶格+对称黑边+弹幕全宽，全屏维持禁用） → **第四轮 1 个提交**：`3e729eb`（弹幕：暂停恢复/缓冲恢复/周期漂移校正改原地续播，不再清空屏幕上弹幕） → **第六轮 1 个提交**：`cb14bf0`（稍后再看删全部/未看完 tab；我的收藏删 7 个分类标签 + 搜索恒固定；两页底纹与稍后再看统一） → **第七轮 1 个提交**：`7cf669e`（主页：其他用户主页头部照搬 PiliPlus 布局；本人主页删硬币/经验/经验条/编辑资料与圆角底纹） → **08-25 归档 3 个提交**：`1867841`（归档I+K轮：竖屏 70% 规格 + 主题默认 + PiP + 切tab，17 文件）→ `31c83f8`（第八轮N：听视频/首页卡/他人主页/动态详情/底纹/长按，24 文件）→ `cb0d6ad`（第九/十轮O+P：恒深色/定位式进度条/歌词双语/按钮右对齐/时长无底色/图文点击，8 文件） → **08-25~08-27 性能/修复 7 个提交**：`d1a2ed7`（消除首页卡片挂载期同步磁盘IO）→ `a872da3`（滑动稳帧-坐标引用化/轮播绘制期变换等）→ `80418d5`（加载提速-封面预加载对齐/删loadMore节流/WBI空闲预刷新/内存缓存扩容）→ `3c66eaa`（过渡动画稳帧-重活推迟morph落定/壁纸去重快照等）→ `fc14a2f`（降后台与空闲开销-轮询门控/采样放宽）→ `3efc07c`（听视频页锁屏误暂停修复）→ **`40e5775`（Q 轮：UI 高/中优先级性能修复，17 文件，见 §2.Q 节）** → **08-28 UI 细节一致性 4 个提交**：`3058da1`（信息流卡片圆角统一 AppShapes Card 级 + 直播卡去孤立阴影 + 骨架卡/空间页同步）→ `f25a75e`（等级徽章配色收敛新建 BadgeLevelPalette）→ `8e1accf`（动态预约/搜索筛选日期时间弹窗统一 AppAlertDialog）→ **`795897c`（播放器画质/倍速/音质菜单主题化，见 §2.S 节）**。前三者与 R5/R6 明细见 §2.R 节

> **手册维护规则**：每完成一轮改动后回写对应节（提交哈希、状态、验证结论）；新功能点并入 §2，验证命令并入 §4，基线失败统一登记在 §4 表格，§8 口诀同步最新口径（避免出现"正文已修订、口诀未跟上"的情况）。

---

## 1. 仓库与环境

| 项 | 值 |
|---|---|
| 项目根目录 | `%USERPROFILE%\Desktop\Bili\BiliPai` |
| JDK | `V:\dev\jdk\jdk-21.0.12+8`（`JAVA_HOME`；旧文档记 `%USERPROFILE%\devtools\jdk-21.0.12+8`，以此新路径为准） |
| Android SDK | `V:\dev\android-sdk`（`ANDROID_HOME` 必须用它，勿用 `<Android SDK>`） |
| Gradle | `V:\dev\gradle-dist\gradle-9.5.0\bin\gradle.bat` |
| miuix 源码 | `V:\dev\miuix`（参数必须带引号：`"-Pbili.miuix.source=V:/dev/miuix"`，PowerShell 会拆坏不带引号的） |
| build-tools (apksigner) | `V:\dev\android-sdk\build-tools\37.0.0` |
| debug keystore | `%USERPROFILE%\.android\debug.keystore`（pass `android`，alias `androiddebugkey`） |
| APK 交付目录 | `%USERPROFILE%\Desktop\新建文件夹 (2)` |

**环境坑（构建时必修）**：
- `ANDROID_HOME=<Android SDK>` 与 `ANDROID_SDK_ROOT=V:\dev\android-sdk` 冲突会导致 miuix 复合构建报错（报错文案 `Could not determine the dependencies of task ':miuix:miuix-shader:compileAndroidMain' → Several environment variables and/or system properties contain different paths to the SDK`）→ 统一 `set ANDROID_HOME=V:\dev\android-sdk`；PowerShell 临时绕过可用 `$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME`（前提 ANDROID_HOME 已是 V:\dev\android-sdk）
- Gradle daemon 偶发崩溃（`Gradle build daemon disappeared unexpectedly`）→ 用 `--no-daemon` 或干净重跑

---

## 2. 代码改动总清单（按功能模块，新版本逐个应用）

### 2.0 复现操作流程（先读这里）

> 适用场景：上游 BiliPai 发布新版本后。按下面 5 步执行，再进 A~P 节逐节应用。**改动清单中的"行号"仅作定位参考，一律以函数名/字符串为锚点（上游改动后行号必然漂移）。**

**步骤 0 · 归档当前工作区（一次性，立即做）**
- 工作区现有 **50 个未提交文件**（I/K/N/O/P 轮改动），**先提交**（建议 3 个提交：①I1~I5+K ②N1~N16 ③O1~O7+P1/P2/P4，每个提交前 `compileDebugKotlin` 通过）或导出 patch 备份——否则上游更新时的 checkout/stash/reset 会让这些改动全部丢失。

**步骤 1 · 拉取并评估上游差异规模**
```bash
git fetch origin
git log --oneline HEAD..origin/main            # 上游新增提交数量与内容
git diff --stat HEAD...origin/main | tail -20  # 改动文件面
```
- 判定：上游只动少数文件 → 走步骤 2a；上游大重构/改动面广 → 走步骤 2b。

**步骤 2 · 选复现策略**
- **2a 小更新**：`git rebase origin/main`（或 merge）后，按 §6.5 的提交序列**逐批 cherry-pick**（17 个关键提交哈希已逐一验证全部存在）；冲突处按对应节"文件 + 逻辑特征"手工解。
- **2b 大更新/重新克隆**：哈希失效，直接在 `origin/main` 上按 A~P 节功能点**手工重做**；每节做完跑该节验证闸门。

**步骤 3 · 逐节应用 + 逐节验证**（应用顺序 A→P；每节最小验证如下，完整命令见 §4）

| 节 | 改动主题 | 最小验证（测试类） |
|---|---|---|
| A/B | 删竖屏播放器+手势+详情页竖屏（最终态见 I 节） | VideoAspectRatioLayoutPolicyTest、VideoDetailLayoutModePolicyTest、VideoPlayerSectionPolicyTest |
| C/D/E | 顶栏实色 13dp 禁模糊、禁壁纸、搜索 UI | HomeHeaderVisualPolicyTest、SearchTabletLayoutPolicyTest |
| F | 评论/弹幕/Tab UI 批 | `:app:compileDebugKotlin` |
| G | 性能两件套（稳定模糊 + 1080P@90） | AppDisplayModePolicyTest、HomeScrollCoordinatorTest |
| H | 可选优化（仅卡顿时做） | 跳过 |
| I | 竖屏 70% 规格 + 主题默认值 | AppThemeConfigTest、BlurIntensityVisualPolicyTest、BlurBudgetPolicyTest、UnifiedBlurPreferencePolicyTest |
| J | 弹幕原地续播 | DanmakuSyncPolicyTest、DanmakuPlaybackSyncPolicyTest |
| K | 切 tab 保持大小 + PiP 方向 | `:app:compileDebugKotlin` |
| L | 列表页精简 | CommonListHeaderCollapsePolicyTest、CommonListAppearancePolicyTest、PersonalListSelectorStructureTest |
| M | 主页（他人 PiliPlus / 本人精简） | `:app:compileDebugKotlin`（SpaceScreenStructureTest 有基线失败，勿修） |
| N | 第八轮（听视频/首页卡/他人主页/动态详情/底纹/长按） | VideoCardCoverStatsLayoutPolicyTest、PlayerInteractionSettingsMappingPolicyTest、HomeSettingsMappingPolicyTest、SpaceLoadPolicyTest、MusicPlayerContentStructureTest |
| O | 第九轮（恒深色/定位式进度条/歌词双语/模糊统一） | MusicPlayerVisualPolicyTest、SubtitleLyricsPolicyTest |
| P | 第十轮（时长无底色/动态点击/按钮右对齐） | HomeSettingsMappingPolicyTest、HomeSettingsStoreParityTest、DynamicCardClickPolicyTest、SpaceHeaderPresentationPolicyTest |
| Q | UI 性能修复批（弹幕key化/全屏层安全区与返回拦截/手势重组治理/动态DataStore上提/坐标惰性/推荐页缓存/关注搜索去抖/Live手势节流） | `:app:compileDebugKotlin` + JankTrackingStructureTest |
| R | 性能轮（滑动/加载/过渡稳帧+后台降耗+听视频锁屏误暂停修复） | ImmersiveStatusBarBackdropPolicyTest、FullscreenPlayerOverlayPollingPolicyTest、VideoPlayerOverlayPolicyTest、PlaybackLifecycleCoordinatorTest |
| S | UI 细节一致性批（卡片圆角统一/徽章配色收口/弹窗统一/播放器菜单主题化） | HomeFeedSkeletonCardStructureTest、StoryVideoCardStructureTest、VideoCardScrollLiteVisualPolicyTest、HardcodedColorMigrationGuardTest |

**步骤 4 · 全量回归**：跑 §4"关键测试"整行命令；失败逐条对照 §4 基线失败清单——**清单外的失败才是本轮引入，务必修复**。

**步骤 5 · 构建/签名/交付/真机验证**：按 §5 流程；真机验证清单逐项过（§5 末节）。

### A. 竖屏播放器：删除 + 禁用触发 + 手势微调（提交 `d94b126` `8b92154` `1f8194e`）

1. **主删（d94b126）**：删除 `feature/video/ui/pager`（13 主 + 12 测）、`overlay/Portrait*`（10 主 + 7 测）、`PortraitDetailPresentationPolicy`、`VideoDetailPortraitOverlayAdapter`、`PortraitStoryNavigationPolicy` 及对应测试。共 66 文件、删 13882 行。
2. **手势（8b92154）**：
   - `VerticalPriorityPagerGesture.kt:37` `PAGER_HORIZONTAL_DOMINANCE_RATIO` 1.5 → 2.0（横滑阈值放宽）
   - `HomePagerSyncPolicy.kt:157` `shouldEnableHomeTopPagerUserScroll` → false（禁首页横滑）
   - `VideoContentSection.kt:257` `shouldEnableVideoContentHorizontalPagerSwipe` → false（禁播放页横滑）
   - 同步更新 2 个测试类
3. **禁用竖屏触发（1f8194e，A1 收尾）**：
   - `VideoDetailSessionPolicy.kt`：`shouldStartInPortraitFullscreenFromRouteHint` / `shouldAutoEnterPortraitFullscreenFromRoute` 恒 false
   - `VideoDetailPlatformPolicy.kt:826` `shouldEnablePortraitExperience` → false
   - `TabletVideoLayout` / `TabletCinemaLayout`：`onPortraitFullscreen = {}`
   - 同步 `VideoDetailLayoutModePolicyTest`

### B. 竖屏视频详情页显示（替代竖屏全屏播放器；提交链 `661f54f`→`da665e6`→`12e7a32`→`7d9d57f`→`36ec3a8`→`18ef5b5`，**以 `18ef5b5` 为最终提交态**）

1. `VideoAspectRatio.kt`：
   - `RATIO_9_16.playerResizeMode`：`RESIZE_MODE_ZOOM` → `RESIZE_MODE_FIT`
   - `toAnime4KDisplayScaleMode()`：`RATIO_9_16` 从 `CROP` 分支移到 `FIT` 分支
2. `VideoPlayerSection.kt`：
   - 提前计算 `isVerticalPlayerVideo`（在 rootModifier 定义前）：`playerState.isVerticalVideo.value || (videoSize.width>0 && videoSize.height>videoSize.width)`
   - 背景去黑边：`.background(if (isVerticalPlayerVideo && !isFullscreen) Color.Transparent else Color.Black)`（左右透出页面背景）
   - 竖屏恒用 `RATIO_9_16`（9:16 竖条布局）
   - 删除原各处重复计算（统一走提前算好的 `isVerticalPlayerVideo`）
   - 封面恢复默认：`aspectRatio(VIDEO_SHARED_COVER_ASPECT_RATIO)`（16:10，不再 9:16 竖条）
3. `VideoDetailPlatformPolicy.kt` `toggleVideoDetailFullscreen()`：`if (activity == null) return` 之后新增 `if (isVerticalVideo) return`（竖屏视频全屏按钮完全无反应）；删除原"竖屏进竖屏全屏"的 `onEnterPortraitFullscreen()` 分支（约 341-348 行）
4. 测试同步：
   - `VideoAspectRatioLayoutPolicyTest.kt`：143 行 `ZOOM`→`FIT`；211 行 `Anime4KDisplayScaleMode.CROP`→`FIT`
   - `VideoDetailLayoutModePolicyTest.kt`：`phoneEnterOrientationPolicy_autoMode_usesVideoDirection` 注释更新

> 历史演进说明（无需复刻中间态）：661f54f 需求3 竖屏恒 9:16 窗口 → da665e6 ZOOM 裁切 → 12e7a32 FILL 填满 → 7d9d57f AUTO 模式固定进横屏全屏 → 36ec3a8 竖屏全屏 ZOOM + 封面 16:10 → **18ef5b5 最终态**：详情页 9:16 竖条 FIT 完整显示无黑边、全屏按钮无反应。
>
> ⚠️ **已被 2026-08-21 第三轮（I 节）推翻**：9:16 竖条顶格方案 → 70% 屏高 + fit 居中 + 上滑联动缩到 16:9。复现以 **I 节**为准（B 节保留仅作演进参考）。

### C. 顶部不透明实色底纹 + 禁用顶部模糊（提交 `2e75aca` `0c9d95a` + 未提交手改）

1. `feature/home/components/HomeHeader.kt`：
   - `continuousSlabHeight = pinnedChromeLayout.blurHeight + 13.dp`（曾改 24dp，后按"缩小 11dp"要求定为 13dp；**新版本直接写 13.dp**）
   - 顶部材质链强制 PLAIN（约 1446-1462 区域，删掉原 `resolveHomeTopChromeMaterialMode` 三次调用）：
     ```kotlin
     val topChromeMaterialMode = TopTabMaterialMode.PLAIN
     val isGlassEnabled = false
     val isTopChromeBlurEnabled = false
     val searchChromeMaterialMode = TopTabMaterialMode.PLAIN
     val isSearchGlassEnabled = false
     val isSearchBlurEnabled = false
     ```
   - slab Box 无条件实色（原约 2068-2092 区域，由"条件性绘制 + 半透明玻璃 homeTopChromeSurface"改为）：
     ```kotlin
     Box(modifier = Modifier.fillMaxWidth().height(continuousSlabHeight)
         .background(MaterialTheme.colorScheme.background))
     ```
   - `resolveHomeTopPinnedChromeLayout()`（约 353-371）：`blurHeight = visibleChromeHeight`（去掉原 PLAIN 时归零逻辑）
2. `feature/settings/screen/BottomBarSettingsScreen.kt`：「顶部模糊」条目 `SettingsSingleChoicePreference(enabled = false, subtitle = "已禁用：顶部底纹固定为不透明实色")`
3. 测试同步：`HomeHeaderVisualPolicyTest.kt:323` `assertEquals(152.dp, plain.blurHeight)`（原 0.dp）

### D. 禁用开屏壁纸作为首页背景（提交 `2e75aca`）

1. `feature/home/HomeScreen.kt`：删除从 SettingsManager 读取壁纸的 collector；`homeWallpaperUri = remember { "" }`（固定为空）
2. `feature/settings/screen/AppearanceSettingsScreen.kt`：「首页壁纸效果」条目 `SettingsSingleChoicePreference(enabled = false)`

### E. 主页/搜索页 UI（提交 `57dcc41`）

1. `HomeHeader.kt`：搜索胶囊、头像、右上角按钮的**边框** → `Color.Transparent`（解析函数保留，测试断言仍用）
2. `SearchScreen.kt`：搜索输入框 `containerColor = Color.Transparent`；聚焦边框透明（`cursorBrush` 仍用 primary）；搜索按钮 `.background(Color.Transparent)`
3. `SearchTabletLayoutPolicy.kt`：手机档 `resultHorizontalPaddingDp` 8 → 16；测试 `SearchTabletLayoutPolicyTest.kt` `compactWidth_usesPhoneDefaults` 断言 8 → 16

### F. 评论/弹幕/Tab UI 批（提交 `0ce8248` `ba983e3` `11c888e` `cd789a4`）

- 评论/二级评论分割线删除；相关推荐底纹透明；弹幕胶囊透明
- Tab 栏固定宽度不跳位（`TabBar` 固定 `itemWidth`，不再 fillMaxWidth 撑满），最新/最热右对齐小号；Tab 栏底部分割线删除
- 弹幕三件套全 Tab 常驻（评论 Tab 不再让位）；顶栏删除最新/最热排序控件；Tab 等宽 88dp 容纳评论数；删除评论列表顶部重复的评论数量标题栏
- 涉及：`VideoContentSection.kt` `CommentSortFilterBar.kt` `RelatedVideoItem.kt` `ReplyComponents.kt` `SubReplyDetailComponents.kt`；同步清理签名与测试断言

### G. 性能（提交 `4de7c6a` `212b58b` + 未提交 P2/P3）

> 背景：`c709c15`/`b51c47a`（批F 滚动感知）与 `5c31155`（响应式采样）把 `isScrollInProgress` 读进 composition + 每卡液态玻璃条件挂载，导致全界面滚动卡顿。根因与 Haze 机制分析见第 3 节。**性能批 = 对它们的止血。**

1. **WS1 滚动性能止血（4de7c6a，已提交）**：
   - `core/ui/blur/UnifiedBlur.kt`：remember key 去掉 `isScrolling`；`resolveBlurBudget` 恒传 `isScrolling=false`（budget 恒稳定，消除滚动起停 RenderEffect 重建）
   - `feature/home/HomeCategoryPage.kt`：`mountedDuringScroll` 改 `remember { Snapshot.withoutReadObservation { gridState.isScrollInProgress } }`（挂载时采样，消除每帧 20-30 卡重组风暴）
   - `feature/home/components/cards/VideoCard.kt`：`liquidModifier` 去掉 `!scrollLiteModeEnabled`，`drawBackdrop` 恒挂载（消除 shader 重建）
   - **保留**：`mountedDuringScroll` 仍喂给 `VideoCardScrollLiteVisualPolicy`（封面阴影/渐变/进度条/统计位）与卡片入场动画（不碰 Haze）
2. **WS2 显示模式锁定 1080P@90（212b58b，已提交）**：
   - `core/ui/performance/AppDisplayModePolicy.kt`（新建）：`AppDisplayModeCandidate` / `resolveAppPreferredDisplayModeId` / `AppDisplayModeEffect`；优先级：① width=1080 & refresh≈90 → ② width=1080（最高刷）→ ③ 当前分辨率下 refresh≈90 → ④不设；minSdk=26 兼容
   - `MainActivity.kt` + `feature/video/VideoActivity.kt`：挂载 `AppDisplayModeEffect(this@...)`
   - `feature/video/screen/VideoDetailPlatformEffectsHost.kt`：全局策略生效时不再提升刷新率（详情页高刷效果让位）
   - `app/src/test/.../AppDisplayModePolicyTest.kt`（新建）：6 个单测
3. **P2+P3（未提交，随 6 个未提交文件一起落盘）**：
   - `HomeHeader.kt:1680` 滚动量化步长 `ExtraSmall(4dp)` → `Small(8dp)`（重组/布局频率减半）
   - `HomeScreen.kt:1414` 嵌入页 top padding 量化同步 4dp → 8dp（防错位）
   - `HomeCategoryPage.kt:212` `remember {}` → `remember(categoryState)`（修 shouldLoadMore 陈旧闭包）

### H. 可选优化（若仍卡顿才做）

- **Tier 2**：升 Haze `2.0.0-alpha03`，`unifiedBlur` 改用内置自适应输入采样（`HazePerformanceMode.Adaptive` / `BlurInputScalePolicy`），删掉手动 `inputScale/blurEnabled` 切换（绘制阶段自动降采样，不切 delegate、不重组）
- `WatchLaterScreen.kt` / `CommonListScreen.kt` / `PartitionScreen.kt` / `DynamicTopBar.kt`：删掉 `c709c15`/`b51c47a` 引入的 `isScrollInProgress` 读进 composition 喂 `unifiedBlur` 的几行（次要重组源；当前未删，若手感无碍可不动）

### I. 竖屏播放器新规格 + 主题默认值（第三轮，2026-08-21，**未提交 17 文件**）

> 目标：详情页竖屏视频播放器 = 状态栏下 → 屏高 70% 区域；视频 FIT 居中（高÷宽判定，1a 上下等高黑边 / 1b 左右等宽黑边）；播放器下方任意位置上滑 → 播放器下沿跟手上移，缩到 16:9 为止（无回弹、下滑恢复）；tab 行固定 + 下沿浅灰分割线；弹幕层覆盖整个播放器区域；主题默认 Material3、默认全关模糊、顶栏默认始终显示。交互参考 PiliPlus（初始 65% 屏高 / 跟手收缩 / tab 贴播放器下沿 / 弹幕全容器）。

#### I1. 播放器区域高度与折叠行程（`VideoDetailScreenStateHolder.kt`，手机竖屏分支，行号约 3120-3155）

```kotlin
val collapsed16x9Height = screenWidthDp * 9f / 16f
val videoHeight = if (isVerticalVideo) {
    (screenHeightDp * 0.7f - playerTopInset).coerceAtLeast(collapsed16x9Height)  // 状态栏下 → 屏高 70%
} else {
    collapsed16x9Height                                                          // 横屏视频恒 16:9
}
```

- `collapseRangePx = (videoHeight - collapsed16x9Height).toPx()`（折叠行程 = 70% 高 − 16:9 高；横屏视频 = 0 不联动）
- `inlinePortraitScrollEnabled = isVerticalVideo`（原硬编码 `false`；仅竖屏视频开启上滑联动）
- `collapsedViewportHeight = collapsed16x9Height`（原 `0.dp`；折叠下限为 16:9，播放器不再完全收起）
- 自动暂停：`playerCollapsedForAutoPause = skipGesturePlayerCollapse`（16:9 折叠态播放器仍可见，不再触发"折叠自动暂停"）

#### I2. 视频 fit 居中几何（`VideoAspectRatio.kt` + `VideoPlayerSection.kt`）

- `VideoAspectRatio.kt`：删除 `resolveVerticalTopAlignedViewportLayout`（顶格），新增：
  ```kotlin
  internal fun resolveVideoFitViewportLayout(
      containerWidth: Int, containerHeight: Int,
      videoAspectRatio: Float,   // 宽/高
  ): VideoViewportLayout {
      // 判定用「高÷宽」方向（与播放器区域同口径）：
      // 视频更横（宽/高 > 容器宽/高）→ 宽度铺满、垂直居中，上下等高黑边（需求 1a）
      // 视频更竖（宽/高 ≤ 容器宽/高）→ 高度铺满、水平居中，左右等宽黑边（需求 1b）
  }
  ```
- `VideoPlayerSection.kt` 主视口（约 3055）：竖屏内联改调 `resolveVideoFitViewportLayout`；`contentAlignment = Alignment.Center`；PlayerView `RESIZE_MODE_FIT`；视频纯相似变换不拉伸
- 播放器高度随上滑联动变化时，黑边自动演化（上下黑边同步渐缩 → 无黑边 → 左右黑边同步渐增），无需额外代码
- 弹幕层（约 3799-3848）：竖屏内联 `danmakuSurfaceModifier = Modifier.fillMaxSize()`（**弹幕覆盖整个播放器区域，含黑边**，与 PiliPlus 一致）；删除 `verticalDanmakuAspectRatio`，viewportLayout 走通用 `resolveVideoViewportLayout`

#### I3. 上滑联动缩小（`VideoDetailScreenStateHolder.kt`，复用既有骨架）

- 骨架原有：`nestedScrollConnection`（约 3258-3294）挂根 Column（约 3296）消费 body 滚动位移 → `inlinePlayerCollapseState.offsetPx` → `inlineViewportHeight = lerp(videoHeight, collapsed16x9Height, progress)`（约 3381-3395）
- 本次仅开启开关 + 改行程/下限（I1）；行为：**body 任意位置向上滑 → 播放器下沿跟手上移；缩到 16:9 停止；继续上滑剩余位移转 body 正常滚动；下滑恢复；松手停住无回弹**
- tab 行内"恢复画面"按钮保留（折叠态显示，点击恢复 70%）

#### I4. tab 固定 + 分割线（`VideoContentSection.kt`）

- 删除 tab 栏跟手折叠全套：`tabBarCollapseConnection`（原约 656-690）、`tabBarCollapsePx`/`tabBarMaxHeightPx`/`commentListAtTop`/`tabBarCollapseEnabled` 及 LaunchedEffect、`tabBarCollapseProgress`/`tabBarVisibleHeightDp`（原约 632-699）；tab 行外层 Box 恒 `wrapContentHeight()`（去掉 `.height(tabBarVisibleHeightDp)` + graphicsLayer clip/alpha/translationY）
- 效果：tab 行（简介/评论分段控件 + 弹幕三件套：开关/发送/设置）**恒全高固定，跟播放器下沿走**（Column 重排天然上移），滚动不再带走 tab
- 分割线：`VideoContentTabBar` 内部 Column 加 `.drawBehind { drawLine(color = MaterialTheme.colorScheme.outlineVariant, start = Offset(0f, size.height - 1.dp.toPx()), end = Offset(size.width, size.height - 1.dp.toPx()), strokeWidth = 1.dp.toPx()) }`（需 `import androidx.compose.ui.draw.drawBehind`；颜色须先提为 `val tabDividerColor = MaterialTheme.colorScheme.outlineVariant`，drawBehind 内不可调用 Composable）
- 发弹幕按钮：`AnimatedVisibility(visible = ...)` 改恒 `true`（原 `shouldShowDanmakuSendInput(isPlayerCollapsed)`，16:9 折叠态空间充足）

#### I5. 主题默认值（需求 4，设置页选项全部保留）

1. **默认 Material3（materialYou）**：
   - `design-system/core/theme/UiPreset.kt`：`AndroidNativeVariant.fromValue` 兜底 `MIUIX→MATERIAL3`；`AppUiStyle.fromLegacyValues` 缺失/非法落点 → MATERIAL3；`resolveUiStyle` iOS 迁移落点 → MATERIAL3；`LocalAppUiStyle` 默认 → MATERIAL3
   - `app/core/theme/Theme.kt`：`PureBiliBiliTheme(uiStyle = AppUiStyle.MATERIAL3)`
   - `app/core/store/SettingsManager.kt`：`AppThemeSettings.uiStyle` 默认 → MATERIAL3
2. **默认全关模糊（BlurIntensity 新增 OFF，全链路）**：
   - `design-system/core/ui/blur/BlurIntensity.kt`：枚举加 `OFF`
   - `BlurIntensityVisualPolicy.kt`：`resolveBlurHazeMaterial(OFF)→THIN`（兜底样式）；`resolveBlurBackgroundAlpha(OFF)→0f`；`resolveBudgetedBlurIntensity` 开头 `if (preferred == OFF) return OFF`（不参与预算降级）
   - `app/core/ui/blur/UnifiedBlur.kt`：提取 `val intensity = currentUnifiedBlurIntensity()`；`blurEnabled = recoverableEnabled && budget.maxBlurLevel > 0 && intensity != BlurIntensity.OFF`
   - `design-system/core/ui/AppThemeConfig.kt`：`blurIntensity` 默认 → OFF
   - `SettingsManager.kt`：`getBlurIntensity` 默认分支 → OFF（`"THIN"` 显式分支保留，`"OFF"` 键值可选）；`HomeSettings.isHeaderBlurEnabled` 默认 false；`headerBlurMode` 默认 ALWAYS_OFF；`resolveHomeHeaderBlurModePreference` 无记录（rawMode==null && legacyEnabled 非 true）→ ALWAYS_OFF；`HomeHeaderBlurMode.fromValue` 兜底 → ALWAYS_OFF；`homeWallpaperEffectMode` 默认 OFF（+`fromValue` 兜底 OFF）；`AppNavigationSettings.miuixTransitionBlurEnabled` 默认 false（数据类 + `mapAppNavigationSettingsFromPreferences` 兜底 `?: false`）
   - `MainActivity.kt` / `VideoActivity.kt`：`blurIntensity` collectAsState `initialValue = BlurIntensity.OFF`
   - `SettingsViewModel.kt`：`blurIntensity` 默认值/暂存 → OFF
   - `feature/settings/ui/SettingsComponents.kt`：`BlurIntensitySelector` subtitle when 加 `BlurIntensity.OFF` 分支（选项列表**不加** OFF——默认关时选择器不显示）
3. **顶栏默认始终显示**：
   - `SettingsManager.kt`：`HomeSettings.homeHeaderCollapseMode` 默认 → `OFF`；`HomeHeaderCollapseMode.fromValue` 兜底 → OFF；`mapHomeSettingsFromPreferences` legacy 兜底 `?: true` → `?: false`
   - `BottomBarSettingsScreen.kt`：`homeHeaderCollapseMode` initialValue → OFF；`headerBlurMode` initialValue → ALWAYS_OFF
4. 测试同步：`design-system/.../AppThemeConfigTest.kt` 默认断言 THIN → OFF

> 注意：以上均为"默认值"改动，**只影响新装/未设置用户**；已有用户存储值不会被覆盖。设置页开关全部保留可开。

### J. 弹幕清空修复：暂停/缓冲恢复/周期漂移校正改原地续播（第四轮，2026-08-23，提交 `3e729eb`，4 文件）

> 目标：① 暂停→播放后弹幕保留（不再清空）；② 连续播放中弹幕不再周期性自动清空；③ 弹幕字体粗细设置项确认已存在（面板"高级渲染"分区「字体粗细」滑杆 1~9 档，无需改动）。
> 涉及文件：`app/.../feature/video/danmaku/DanmakuPlaybackSyncPolicy.kt`、`DanmakuManager.kt` + 测试 `DanmakuSyncPolicyTest.kt`、`DanmakuPlaybackSyncPolicyTest.kt`

**根因**：两个清空路径都走 `HardResync`（`executeExplicitDanmakuResync` = pause → **clear** → setData → start）：
1. 暂停→播放：`onIsPlayingChanged(true)` 恢复时 HardResync，`clear()` 把暂停时冻结在屏幕上的弹幕全部清掉（暂停本身是 `PauseOnly` 只 pause 不清屏，弹幕停在原位）。
2. 播放中周期性清空：`startDriftSync()` 漂移校正循环，`resolveDanmakuGuardAction` 到 tick 周期（1x 每 6 tick×3.2s≈19s；非 1x 每 3 tick；≥1.75x 每 9 tick）返回 HardResync → `clear()+setData` 重建，表现为"播放一段时间后弹幕不定期自动清空"。

**修复**（核心：引擎 setData/start 均不清屏，恢复/漂移只需 `start(position)` 重定引擎时钟，即 `softResyncDanmakuTimeline`）：

1. `DanmakuPlaybackSyncPolicy.kt`（策略层，全部由 HardResync 改 SoftResync）：
   - `resolveDanmakuActionForIsPlayingChange`：播放恢复 → `SoftResync`（原地续播）
   - `resolveDanmakuActionForPlaybackState`：`STATE_READY` 缓冲恢复 → `SoftResync`
   - `resolveDanmakuActionForForegroundRecovery`：活动会话恢复 → `SoftResync`
   - `resolveDanmakuGuardAction`（周期漂移）：**恒返回 `SoftResync`**（原来只有 ≥1.75x 高倍速才软同步，1x/非 1x 是 HardResync 清屏）；tick 周期判定 `shouldForceDanmakuDataResync` 不变
2. `DanmakuManager.kt`（调用点）：
   - `onIsPlayingChanged` / `onPlaybackStateChanged` 的 SoftResync 分支：`softResyncDanmakuTimeline(position, shouldPlay=true, reason="is_playing_changed"/"state_ready_resume")` + `isPlaying=true` + `startDriftSync()`；HardResync 分支保留作兜底（同样降级为原地续播，防未来策略回归清屏）
   - `startDriftSync()` 漂移循环：SoftResync/HardResync 统一走 `softResyncDanmakuTimeline(..., reason="drift_sync")`
   - `recoverAfterForeground()`：前台恢复同样原地续播
3. **保留硬清屏的场景（有意保留，勿改）**：`onPositionDiscontinuity` seek（拖进度条/跳转）、`seekTo()` 手动 seek、`prepareForSeekScrub`、`loadDanmaku` 新 cid、`hide()` 关弹幕、设置变更 `applyConfigToController`——这些是真正的"时间线跳变"，需要清屏重建。
4. 测试同步：`DanmakuSyncPolicyTest` ×4（isPlayingChange/缓冲恢复/前台恢复/漂移 guard 断言 HardResync → SoftResync）、`DanmakuPlaybackSyncPolicyTest` ×1（guard 任意倍速均 SoftResync）。**其余策略（seek 抑制、attach 重放、显式 resync 顺序等）未动。**

---

### K. 切 tab 保持播放器大小 + PiP 宽高比随视频方向（第五轮，2026-08-23，**未提交**：`PipAspectRatioPolicy.kt` 新建 + `VideoDetailScreenStateHolder.kt`/`MainActivity.kt` 修改）

> 目标：① 竖屏视频浏览评论时把播放器滑小，切到「简介」tab 后**播放器保持当前大小**（不再弹回 70%）；② 系统画中画宽高比随视频方向：横屏视频用横屏 PiP（16:9），竖屏视频用竖屏 PiP（9:16）；③ 「显示画中画后最近任务中无 BiliPai」经排查为系统/ROM 行为，无代码改动（详见 K3）。

#### K1. 切 tab 不再重置播放器折叠（问题 1，`VideoDetailScreenStateHolder.kt` 手机竖屏分支）

**根因**：旧逻辑（第三轮 I 节遗留）在 tab 切换时强制重置播放器高度：

```kotlin
// ❌ 已删除：compact 标志恒 false 时，切 tab（selectedVideoContentTabIndex 变化）→ 重启 LaunchedEffect
// → 条件成立 → inlinePlayerCollapseState.reset() → 播放器从当前大小弹回 70% 最大
LaunchedEffect(selectedVideoContentTabIndex, compactInlinePlayerForCommentTab, compactInlinePlayerForIntroScroll, portraitPlayerCollapseMode) {
    if (!compactInlinePlayerForCommentTab && !compactInlinePlayerForIntroScroll) {
        inlinePlayerCollapseState.reset()
    }
}
```

**修复**（删除/简化以下，恢复"切 tab 保持播放器当前折叠大小"）：
1. 删除上述 LaunchedEffect 及 `compactInlinePlayerForCommentTab = false`、`compactInlinePlayerForIntroScroll = false` 两个恒 false 标志
2. 删除 `portraitPlayerCollapseMode` 的 DataStore 读取（`getPortraitPlayerCollapseMode` 已无消费方；**设置页 PlaybackSettingsScreen 的「评论上滑缩小播放器」开关保留不动**）
3. `skipGesturePlayerCollapse` 恒 `false`（原 `shouldSkipGesturePlayerCollapseForLayout(...)` 调用删除；竖屏联动正常消费位移）
4. `commentTabCollapseProgress` 动画（`animateFloatAsState` 300ms）删除，改恒 `0f`——其 target 本就恒 0（compact 标志恒 false），且 `effectiveCollapseProgress = collapseProgress` 未消费它
5. 删除 import `shouldSkipGesturePlayerCollapseForLayout`（policy 函数 `VideoDetailScrollCoordinator.kt:62` 定义保留，勿删）

**保留不动**：`LaunchedEffect(Unit) { commentBackToTopRestoreFlow.collect { restore() } }`（评论区一键回顶仍恢复播放器）、返回 morph 时 `expandPlayerForSharedReturn` 的 reset（3380 附近，返回展开语义保留）。

#### K2. PiP 宽高比随视频方向（问题 3）

**新建** `feature/video/player/PipAspectRatioPolicy.kt`：

```kotlin
internal fun resolvePipAspectRatio(player: Player?): Rational {
    val size = player?.videoSize ?: return Rational(16, 9)
    if (size.width <= 0 || size.height <= 0) return Rational(16, 9)
    return if (size.height > size.width) Rational(9, 16) else Rational(16, 9)
}
```

**调用点（3 处，均在 `MainActivity.kt`，替换原 `Rational(16, 9)`）**：
1. `onUserLeaveHint` 进入 PiP（2100 附近，`shouldTriggerPip` 分支）
2. `enterMiniPlayerPictureInPicture()`（2130 附近，小窗→PiP 按钮）
3. PiP 激活后 `LaunchedEffect(isInPipMode, isPlaying)` 里 `setPictureInPictureParams`（1400 附近）

同时删除 `MainActivity.kt` 的 `import android.util.Rational`（已无引用）。

**未改**：`VideoActivity.kt` `buildPipParams` 仍 16:9——VideoActivity 内播放器由 `VideoDetailScreen` 内部持有（viewModel 无公开 player 访问器），且非主路径（主详情页在 MainActivity），复现时保持现状即可；直播 PiP（`LivePlayerScreen.kt`）恒 16:9 合理（直播画面）。

#### K3. 「显示 PiP 后最近任务中无 BiliPai」（问题 2，诊断结论，无代码改动）

排查结果（代码层排除）：
- 全仓无 `moveTaskToBack` / `removeTask` / `finishAndRemoveTask` / `excludeFromRecents` / `FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS`
- Manifest：MainActivity/VideoActivity 均无 `excludeFromRecents`/`noHistory`，`supportsPictureInPicture="true"`、launchMode standard/singleTop 正常
- 进入 PiP 走标准 `enterPictureInPictureMode`（`onUserLeaveHint`），`onPictureInPictureModeChanged` 仅记录状态

**结论**：系统 PiP 显示后最近任务不显示应用为**设备 ROM 行为**（国产 ROM 常见，会把 PiP 任务从最近任务隐藏）；若设备「画中画」权限未授予，`enterPictureInPictureMode` 会静默失败，此时看到的"悬浮窗"实为应用内小窗（overlay，recents 中无独立卡片属正常）。**复现/验证时先确认：设置→应用→BiliPai→画中画权限已开启，且「后台播放模式」选的是"画中画"（SYSTEM_PIP）**。代码无需改动。

---

### L. 列表页精简：稍后再看删 tab + 我的收藏删分类标签、搜索恒固定、底纹统一（第六轮，2026-08-23，提交 `cb14bf0`，6 文件）

> 目标：① 稍后再看删除「全部」「未看完」tab，搜索框与 tab 共享的半透明底纹范围随之下移收缩；② 我的收藏删除「视频、番剧、影视、文章、笔记、话题、课程」7 个分类标签，卡片上移补位；③ 我的收藏搜索控件始终固定，不随列表移动；④ 两页底纹透明度/颜色/范围统一，首卡距底纹下沿约 8dp。
> 涉及：`feature/watchlater/WatchLaterScreen.kt`、`feature/list/CommonListScreen.kt`、`feature/list/CommonListAppearancePolicy.kt` + 测试 `CommonListHeaderCollapsePolicyTest.kt`、`PersonalListSelectorStructureTest.kt`、`CommonListAppearancePolicyTest.kt`

**1. 稍后再看（WatchLaterScreen.kt）**：
- 删除 `WatchLaterFilter.entries.forEach` 的 FlowRow（全部/未看完 chips）+ 尾部 `Spacer(8dp)` 与 `AppFilterChip` import
- 底纹 Box 为 wrap-content（Scaffold topBar 槽内 `unifiedBlur` Box），删 chips 后**自动收缩为「标题栏 + 搜索框」范围**，无需改底纹代码；列表 contentPadding `top = 底纹实测高 + AppSpacingTokens.Small(8dp)` → 首卡距底纹下沿恒 8dp，原来约 40dp 的 chips 区还给列表
- `WatchLaterFilter` 枚举、VM 的 `selectFilter`/`tabCache` 保留未动（无 UI 入口，数据层 `filter.viewed` 参数不受影响）

**2. 我的收藏（CommonListScreen.kt）**：
- 删除 `FavoriteSection.entries.forEach` 的 FlowRow（7 个分类 chips）+ 尾部 Spacer；`favoriteSection` 状态保留（恒 VIDEO），非 VIDEO 分支成为死代码（不删，避免牵连 FavoriteCategoryScreen 等），不影响编译
- **卡片上移补位**：顶栏高度由 `onGloballyPositioned` 实测（headerHeightPx → headerHeightDp，作为列表 contentPadding top），chips 删除后自动收缩，卡片自然上移
- **搜索恒固定**：`resolveCommonListHeaderCollapseModeForScreen` 对 `isFavoritePage=true` 恒返回 `ALWAYS_VISIBLE`（原为 SHOW_ON_REVERSE_SCROLL → SHOW_AT_TOP_ONLY），顶栏（标题+搜索+底纹）不再随滚动折叠/平移，与稍后再看口径一致；历史记录页仍遵循用户设置
- **底纹统一**：`topBarBackgroundModifier` 删除 `.background(headerBackgroundColor)` 链——原来模糊关闭时整块不透明 surface、开启时再叠加 surface 半透明；现在与稍后再看完全一致，仅 `unifiedBlur`（模糊开 = 毛玻璃 + Haze 材质半透明染色，模糊关 = 全透明）；搜索框删除 `containerColor = surfaceVariant.copy(alpha = 0.22f)` 覆盖 → 用 AppSearchField 默认 `surfaceVariant.copy(alpha = 0.5f)`，与稍后再看一致
- `CommonListFavoriteHeaderLayout.headerBackgroundAlphaMultiplier` 字段删除（不再需要，同步删 3 档赋值）；`headerBottomPaddingDp` 由 6/6/4 统一为 **8**（底纹下沿 → 首卡间隙 = 8dp，与稍后再看同口径）
- 清理：删除 `blurIntensity`/`backgroundAlpha`/`headerBackgroundAlpha`/`globalWallpaperVisible`/`shouldUseHeaderLocalBlur`/`headerBackgroundColor` 变量及 `BlurStyles`/`currentUnifiedBlurIntensity`/`LocalGlobalWallpaperBackdropVisible`/`resolveGlobalWallpaperChromeColor` import；策略函数 `resolveCommonListHeaderBlurEnabled`/`shouldUseCommonListHeaderLocalBlur` 保留（`CommonListAppearancePolicyTest` 仍测）
- ⚠️ `surfaceType = BlurSurfaceType.HEADER` 保留（仅影响模糊预算，结构测试 `commonListTopBar_usesHeaderBlurBudget` 断言它）

**3. 测试同步**：
- `CommonListHeaderCollapsePolicyTest`：`favorite header only returns after reaching the top` → 改 `favorite header stays always visible`，favorite 页断言 ALWAYS_VISIBLE（两种配置），非 favorite 页仍透传
- `PersonalListSelectorStructureTest`：`favoritePrimarySelectors_areTapFirstAndAlwaysReachable` → 改 `favoriteCategoryChips_areRemovedFromHeader`（断言 CommonListScreen 无 `FavoriteSection.entries.forEach`）；`secondaryPersonalFilters_doNotRequireHorizontalDrag` 中 watchLater 断言 `assertTrue(WatchLaterFilter.entries.forEach)` → `assertFalse`
- `CommonListAppearancePolicyTest`：两处 `assertTrue(layout.headerBackgroundAlphaMultiplier < 1f)` → 改 `assertEquals(8, layout.headerBottomPaddingDp)`

---

### M. 主页改造：其他用户主页头部照搬 PiliPlus + 本人主页精简（第七轮，2026-08-23，提交 `7cf669e`，3 文件）

> 目标：① 其他用户主页（SpaceScreen 头部）照搬 PiliPlus `pages/member/widget/user_info_card.dart` + `header_layout_widget.dart` 的布局；② 本人主页删除 硬币数量/经验值/经验条/编辑资料，下方按钮上移；③ 删除本人主页从「离线缓存/历史/订阅/稍后再看」到「投稿预览」的圆角矩形底纹（含顶边线段、阴影线段）。
> 涉及：`feature/space/SpaceScreen.kt`、`feature/profile/ProfileScreen.kt` + 测试 `SpaceScreenStructureTest.kt`

**1. 其他用户主页头部（SpaceScreen.kt `SpaceHeader`，PiliPlus 布局）**：
- **几何**：hero 156dp 不变；头像 80dp 左下，**伸入 banner 20dp、底部伸出 60dp**（PiliPlus `kAvatarTopPadding=115/135` 口径换算：overlap 20dp、extend 60dp，`Box` 总高 156+60=216dp）；头像加 PiliPlus 式 2dp surface 描边圆环（`.border(2.dp, surface, CircleShape)`）
- **右侧栏（PiliPlus actions 槽）**：`Alignment.BottomEnd` 列 = **粉丝/关注/获赞 stats 行**（数字 14sp + 标签 12sp，间距 8dp，竖线分隔 15dp）+ **私信/关注按钮行**（非本人时显示），浮于 banner 底部右下方——原来 stats 与头像同行、按钮与名字同行的布局作废
- **名字行**：名字 + 等级徽标 + VIP 徽标独占一行（私信/关注已移入右侧栏）；直播中 chip / 官方认证 / sign / UID 依次向下（不变）
- stats 尺寸同步 PiliPlus：`SpaceHeaderStat` 17sp/13sp → 14sp/12sp，`SpaceHeaderMetricDivider` 30dp → 15dp
- 保留：banner 图片/渐变/scrim、直播角标、滚动视差折叠、`SpaceHeaderRelationActions` 组件本体、Tab 行与内容全部不变
- ⚠️ 数据层缺口（照搬时已知）：充电/大航海（charges/guards）、共同关注（followingsFollowedUpper）、头像挂件（pendant）、合集多图 banner（collectionTop）、PR 横幅（prInfo）、粉丝勋章墙（live medal）——BiliPai 聚合 API 无这些字段，布局就现有数据渲染，复现时无需补

**2. 本人主页删除项（ProfileScreen.kt）**：
- `ProfileSpaceHeader`：删除 硬币文案（1520-1524 旧行）、经验文案 + 经验条 `AppLinearProgressIndicator`（旧 1514-1537 整块）、**编辑资料** `AppOutlinedButton`（旧 1546-1569 Row；`showWallpaperAction` 时仅保留壁纸菜单按钮，改 `Arrangement.End` 右对齐）；`levelProgress` 计算一并删除（policy 函数 `resolveProfileLevelProgress` 保留，`ProfileDashboardPolicyTest` 仍测）
- 删除编辑资料入口链：`onEditClick` 参数（ProfileSpaceHeader/ProfileSpaceHeroHeader/两处调用点）、`showEditDialog` 状态与弹窗块、`ProfileEditAccountDialog` + `ProfileReadonlyAccountField` 函数、`onSignSave` 参数与传参
- **底纹删除**：手机登录态 `ProfileContentSheet`（AppSurface 圆角容器：顶部 20dp 圆角 + 0.5dp 顶边线段 = 用户所说的「夹在两个圆角之间的线段」+ `shadowElevation` = 底部阴影线段）整体移除 → 内容（快捷入口四宫格 + tabs + tab body 至投稿预览）改为平铺在页面背景上；`ProfileContentSheet` 函数删除；布局/Chrome policy 字段（contentSheetTopRadiusDp 等、sheetBorderColor 等）保留未动
- 效果：hero 删四项后高度收缩 + 底纹移除 → **四宫格/tabs/投稿预览等整体上移**（LazyColumn item 高度自动）；平板 rail（无底纹）只受影响前项
- 清理：`AppLinearProgressIndicator`/`AppOutlinedTextField` import 删除；旧布局（游客/平板 legacy）不动

**3. 测试同步**：
- `SpaceScreenStructureTest.kt`：`space follow actions share the name and level row` → 改名 `space follow actions sit in the piliplus banner right column`，断言改为 `右侧栏（PiliPlus actions 槽）：粉丝/关注/获赞 + 私信/关注按钮行` 注释存在
- 其余 profile/space policy 测试全绿（87 用例仅 2 个既有基线失败，见 §4）

---

### N. 第八轮：听视频批 + 首页卡片 + 他人主页批 + 动态详情 + 列表页底纹 + 长按倍速（2026-08-25，**未提交**）

> 目标：① 听视频播放页浅色/深色视觉完全一致（前景恒白）、禁横滑切 tab（切换条常显）、歌词页接入视频字幕（官方歌词→第三方→原生字幕→AI字幕→"未找到匹配歌词"）、听视频列表点击直达播放页、进度条改增量拖动（播放头不突变、点击不跳转）；② 首页视频卡片统计贴封面默认开启（播放量+弹幕左、时长右，去掉封面在线人数 pill）、首页去掉"发布于…"；③ 他人主页：投稿默认单列（可手动切回并记忆）、删 tab 行下全宽分隔线、统计项照抄我的主页（72dp 固定宽居中）、私信/关注按钮行与统计行等宽、右侧列与头像中线对齐、顶栏实色底纹（删搜索横条但保留顶栏搜索功能）、去大会员标识、动态tab列表隐藏三件套+卡片分隔线（详情页保留）；④ 动态详情：评论数加粗、删其下分隔线、最热/最新切换条 48→36dp、整页字体 +2sp；⑤ 历史记录页顶栏+搜索+分类恒固定且底纹实色、稍后再看页顶栏+搜索底纹实色；⑥ 长按倍速默认 3.0x + 长按开始瞬间震动。

**N1. 听视频播放页配色恒白（需求 2.1，`MusicPlayerContent.kt`）**
- `resolvedContentColor` 恒 `Color.White`（原按封面 Palette 亮度阈值切换 onSurface/White 的逻辑不再调用）；背景（Palette 取色 + 封面模糊渐变）保留，强调色仍主题 primary。函数 `resolveMusicPlayerContentColor` 与其测试保留未动。

**N2. 禁横滑切 tab + 切换条常显（需求 2.2，`MusicPlayerContent.kt`）**
- COMPACT_PAGER 的 `HorizontalPager` 加 `userScrollEnabled = false`（仅点按切换）。
- 原 `AnimatedVisibility(visible = pagerState.currentPage != 1)`（歌词页隐藏切换器）删除，`BottomBarLiquidSegmentedControl` 恒常显。
- 歌词页布局抬升：浮动控制面板 `padding(bottom = 76.dp)`（切换条 44dp+16dp 边距 + 16dp 间距）、`LyricsImmersiveProgress` 同步 bottom=76dp、歌词列表 contentPadding bottom 260 → **336dp**。

**N3. 歌词页字幕链路（需求 2.3，`MusicViewModel.kt` + 新建 `SubtitleLyricsPolicy.kt`）**
- 听视频歌词优先级：**B站官方歌词 → 网易云/QQ/酷狗 → 原生字幕 → AI字幕 → "未找到匹配歌词"**（AU 音乐模式链路不变）。
- `MusicViewModel`：`loadLyricsForVideo` 第三方失败（NotFound/Failed）后走 `loadVideoSubtitleLyrics`：`VideoRepository.getPlayerInfo(bvid,cid)` → `mapPlayerInfoSubtitleTracks` 排序 → 优先非 AI 轨道（`isLikelyAiSubtitleTrack`），无原生用 AI → `getSubtitleCues` 下载解析 → `buildLyricDocumentFromSubtitleCues`（新建策略：cue → LyricLine，保留起止时间支持点击行 seek/高亮/自动滚动）→ 写 FileLyricsCache；`retryLyrics` 对听视频重走整条链路（记录 `lastVideoLyricTarget`）。
- 无字幕/纯音乐 → 现有空态文案"未找到匹配歌词"（听视频与 AU 音乐共用，无改动）。

**N4. 听视频点击直达播放页（需求 3.1，`AppNavigation.kt`）**
- `onNowPlayingClick` 与 `onPlayTracks`：`BiliPaiNavKey.VideoDetail(startAudio=true)` 改为直接 `BiliPaiNavKey.AudioMode(sourceBvid, sourceCid, 0)`；`PlaylistManager.setExternalPlaylist` 保留。返回直接回听视频列表页；视频详情页的"听"入口不受影响。

**N5. 进度条增量拖动（需求 3.2，`MusicPlayerContent.kt` `MusicProgress`）**
- 自定义轨道+拇指（原 `AppSlider` 移除）：`pointerInput + detectDragGestures`，拖动开始记录 `dragBasePositionMs`，仅按 `dragAmount.x / trackWidthPx * duration` 累积 `dragDeltaMs`（clamp 到 [-base, duration-base]），播放头 = base+delta **不吸附手指、不突变**；松手才 `onSeek`；**点击不跳转**；时间标签同步预览。AU 音乐共用生效。`drawBehind` 内颜色须先取局部值（DrawScope 非 @Composable 上下文）。

**N6. 首页卡片统计贴封面默认开启 + 去封面在线人数（需求 4，`SettingsManager.kt` / `VideoCard.kt` / `VideoCardCoverStatsLayoutPolicy.kt`）**
- `compactVideoStatsOnCover` 默认 false → **true**（数据类 + Preferences 读取 + Flow 三处）；`homeDurationStyle` 默认保持 OUTSIDE_COVER（时长 pill 在统计行内右对齐）。
- 封面紧凑统计行删除在线人数 pill 渲染与布局预算（`showOnlineCount` 字段、`hasOnlineCount` 参数、`COMPACT_ONLINE_BADGE_MIN_WIDTH_DP` 全删）；播放量+弹幕左对齐、时长右对齐，左右 8dp / pill 间距 6dp 不变。封面下方统计行（贴封面设置关闭时）的在线人数保留。
- 仅改默认值，已设置用户不受影响。

**N7. 首页去掉"发布于…"（需求 5，`HomeCategoryPage.kt`）**
- 两处 `showPublishTime = true` → `false`（首页推荐流 + 分类页）；搜索页、相关推荐、故事卡保留。

**N8. 他人主页投稿默认单列（需求 6，`SpaceLoadPolicy.kt`）**
- `defaultSpaceContributionVideoLayoutMode()` GRID → `SINGLE_COLUMN`；顶栏菜单"切换为双列/单列"保留，切换结果仍 rememberSaveable 记忆。

**N9. 删 tab 行下全宽分隔线（需求 7，`SpaceScreen.kt`）**
- `SpaceMainTabRow` 内 `AppHorizontalDivider`（原 tab 行下方全宽线）删除；选中 tab 的短指示条保留；分类标签/搜索/视频列表自然上移约 11dp。

**N10. 他人主页头部统计与按钮（需求 8.1/8.2/9，`SpaceScreen.kt`）**
- `SpaceHeaderStat` 照抄我的主页 `ProfileSpaceStat`：固定宽（窄屏 <360dp 用 60dp，否则 72dp）+ 内容居中，数字 `titleMedium` Bold + 标签 `labelSmall`；三项保持 粉丝/关注/获赞；分隔线 15dp、间距 8dp 不变；整体仍在 banner 右下角右侧对齐。
- `SpaceHeaderRelationActions`：Row `fillMaxWidth()`（与统计行等宽），私信 32dp 圆钮左对齐、关注按钮 `weight(1f)` 拉长右对齐，两按钮间距 8dp 不变。
- 右侧栏 Column 垂直对齐口径：`align(TopEnd)` + `onSizeChanged` 测高 + `offset` 使整体中线与头像中线重合（头像中线 = hero156 + extend60 - 40 = 176dp）；删除原 `align(BottomEnd) + bottom=12dp`。

**N11. 他人主页顶栏实色 + 删 tab 搜索横条（需求 10/11，`SpaceScreen.kt`）**
- 顶栏 Box：`unifiedBlur` → `background(MaterialTheme.colorScheme.background)` 实色；删除 haze 基础设施（`hazeState`/`hazeSourceCompat`/`isSpaceScrolling`/相关 import）。**顶栏搜索按钮保留**。
- 动态 tab 与投稿 tab 的 `SpaceSearchEntryChip` 常显横条删除（组件函数一并删），搜索模式 `AppSearchField` 输入框保留（顶栏放大镜进入）；`contributionVideoItemStartIndex` 计数同步去掉 chip 项。

**N12. 他人主页去大会员 + 动态卡列表精简（需求 12.1/12.2/13，`SpaceScreen.kt` / `DynamicCard.kt` / `DynamicScreen.kt`）**
- 名字恒 `onSurface`（不再 VIP secondary 色），`UserVipBadge` 渲染删除；等级徽标保留。
- `DynamicCardV2` 新增参数 `showActionBar: Boolean = true`：为 false 时隐藏转发/评论/点赞三件套行与卡片间分隔线（`!isDetail` 分支同步加条件）。调用点：根动态 tab（`DynamicScreen.kt`）与他人主页动态 tab 传 `false`；动态详情页（`DynamicDetailScreen.kt`）保持默认 true。根动态页"新旧动态分隔条"保留。

**N13. 动态详情页评论头 + 整页字体（需求 14/15，`DynamicCommentSheet.kt` / `DynamicDetailScreen.kt`）**
- `DynamicInlineCommentHeader`：评论数 `fontWeight` SemiBold → **Bold**（文案仍"N条回复"）；其下 `AppHorizontalDivider` 删除；`resolveDynamicCommentSortControlSpec` 高度 48 → **36dp**（文字仍垂直居中），宽度 58dp 不变。
- 详情页整页 +2sp：Success 分支包 `MaterialTheme(colorScheme/shapes/typography 透传, typography = 放大版)`，放大版由新增扩展 `Typography.withFontSizeDelta(deltaSp)` 生成（全样式 fontSize/lineHeight +2sp；`LocalTypography` 为 internal 不可直接注入）。⚠️ TextUnit 加法定不能用 `fontSize + x.sp`（该 Compose 版本需显式 import 且与 Offset.plus 冲突），用 `(fontSize.value + deltaSp).sp`。

**N14. 历史记录页 / 稍后再看页底纹（需求 16/17，`CommonListScreen.kt` / `CommonListAppearancePolicy.kt` / `WatchLaterScreen.kt`）**
- `resolveCommonListHeaderCollapseModeForScreen` 新增 `isHistoryPage` 参数：历史页恒 `ALWAYS_VISIBLE`（与收藏页同口径，忽略用户"上滑显示"设置）。
- `CommonListScreen.topBarBackgroundModifier`：历史页（`historyViewModel != null`）改 `background(MaterialTheme.colorScheme.background)` 实色，覆盖顶栏+搜索横条+分类条三块（到分类条下沿）；其余共用列表（含我的收藏）保持 unifiedBlur 透明底纹。
- `WatchLaterScreen`：顶栏 Box `unifiedBlur` → `background(MaterialTheme.colorScheme.background)` 实色（覆盖顶栏+搜索框）；删除 haze 基础设施与相关 import（`globalHazeState: HazeState?` 参数类型保留，import `dev.chrisbanes.haze.HazeState` 需保留）。

**N15. 长按倍速 3x + 震动（需求 18，`PlaybackSpeedPolicy.kt` / `SettingsManager.kt` / `VideoPlayerSection.kt`）**
- `DEFAULT_LONG_PRESS_SPEED` 2.0 → **3.0**（选项 1.5/2/2.5/3 不变）；`PlayerInteractionSettings.longPressSpeed` 数据类默认 2.0f → 3.0f（Preferences 读取用常量，自动跟随）。
- `startLongPressSpeedGesture` 成功启动后加一次 `performHapticFeedback(HapticFeedbackType.LongPress)`（长按开始瞬间）；锁定/解锁震动与长按锁定机制全部保留。

**N16. 测试同步（7 文件）**
- `VideoCardCoverStatsLayoutPolicyTest`：删 hasOnlineCount/showOnlineCount 断言，删"在线人数保留"用例。
- `PlayerInteractionSettingsMappingPolicyTest`：空偏好默认 `longPressSpeed` 断言 2.0 → 3.0。
- `HomeSettingsMappingPolicyTest`：空偏好 `compactVideoStatsOnCover` 断言 false → true。
- `SpaceLoadPolicyTest`：默认布局断言 GRID → SINGLE_COLUMN（用例名同步）。
- `SpaceScreenStructureTest`：搜索用例删 `SpaceSearchEntryChip(`/`borderedContainer(Field)` 断言（改断言 `assertFalse(SpaceSearchEntryChip(`），`onPrimaryClickOverride` 断言换为现存符号。
- `MusicPlayerContentStructureTest`：歌词页 bottom 断言 260 → 336dp。
- `CommonListHeaderCollapsePolicyTest`：新增历史页恒 ALWAYS_VISIBLE 用例。
- ⚠️ 既有失败（勿修）见 §4 基线失败清单（HomeSettingsMappingPolicyTest ×3、SpaceScreenStructureTest showTabRail、MusicPlayerLayoutPolicyTest compact artwork、settings-core 单测编译问题）。

---

### O. 第九轮：听视频页恒深色 + 进度条恢复定位式 + 歌词双语/模糊 + 他人主页按钮行等宽（2026-08-25，**未提交**）

> 目标：① 听视频页浅色模式下文字不可见 → 整页恒定深色模式 UI（含全部弹层/系统栏图标）；② 进度条撤销增量式改回"定位式"（点击/拖动直接定位，**推翻 N5**）；③ 歌词双语：原生字幕非中文 → 每句显示中外双语对照，原生中文只显示中文；④ 非当前歌词模糊系数调低（更清晰但仍有层次），随后再统一为非当前行全体同相邻句样式（O7）；⑤ 他人主页私信/关注按钮行水平宽度与统计行等宽（修正 N10 的 fillMaxWidth 描述）。
> 涉及：`AudioModeScreen.kt`、`MusicPlayerContent.kt`、`SpaceScreen.kt`、`SubtitleLyricsPolicy.kt`、`MusicViewModel.kt`、`MusicPlayerVisualPolicy.kt` + 测试 `MusicPlayerVisualPolicyTest.kt`、新建 `SubtitleLyricsPolicyTest.kt`

**O1. 听视频页整页恒深色（需求 1b，`AudioModeScreen.kt`）**
- `AudioModeScreen` 改为包装函数：内部 `MaterialTheme(colorScheme = darkColorScheme())` + `Box(fillMaxSize().background(darkColorScheme().background))` 包裹 `AudioModeScreenContent`（原函数体改名，签名不变）——整个页面（播放页/歌词页/操作与队列/歌词搜索/歌词设置弹层/加载态/定时关闭对话框）强制深色 ColorScheme，与系统深色模式下显示完全一致；背景仍随封面 Palette + 深色渐变遮罩（用户确认保留）。
- 新增 `ForceAudioModeSystemBars()`：DisposableEffect 强制 `isAppearanceLightStatusBars`/`isAppearanceLightNavigationBars = false`（浅色图标 = 深色模式样式），onDispose 恢复原值。
- 入口全部走 `AudioModeScreen`（AppNavigation AudioMode 路由、MusicDetailScreen DASH 音轨入口）自动生效，无需其他改动。

**O2. 进度条恢复定位式（需求 1a，`MusicPlayerContent.kt` `MusicProgress`）**
- ⚠️ **推翻 N5**：删除自绘 `pointerInput + detectDragGestures` 增量式整块（含 `trackWidthPx`/`dragBasePositionMs`/`dragDeltaMs` 状态与 `drawBehind` 轨道，及新增的 detectDragGestures/mutableLongStateOf/drawBehind/Offset/StrokeCap/pointerInput/onSizeChanged import），恢复原 `AppSlider` 实现（`value = draggedPosition ?: positionMs`，`onValueChange` 拖中预览、`onValueChangeFinished` 松手 seek；点击/拖动播放头直接跟随手指 = 定位式）。

**O3. 他人主页按钮行与统计行等宽（需求 2，`SpaceScreen.kt`）**
- ⚠️ **修正 N10 表述**：`SpaceHeaderRelationActions` 的 Row 不再 `fillMaxWidth()`（那等于整行宽度、比统计行宽），改为**实测统计行宽度**传入——`SpaceHeader` 右侧栏统计 Row 加 `.onSizeChanged { metricsRowWidthPx = it.width }`，按钮行调用点传 `modifier = Modifier.width(with(density) { metricsRowWidthPx.toDp() })`。
- 布局不变：私信 32dp 圆钮左对齐、关注按钮 `weight(1f)` 拉长右对齐、两按钮间距 8dp、纵坐标（Column 中线与头像中线对齐）不变。

**O4. 歌词双语对照（需求 1，`SubtitleLyricsPolicy.kt` / `MusicViewModel.kt`）**
- `SubtitleLyricsPolicy.kt`：新增 `isChineseSubtitleTrack(track)`（lan 小写以 `zh` 开头、等于 `cn` 或以 `cn-` 开头，或 lanDoc 含 中文/简体/繁体/国语/普通话）；`buildLyricDocumentFromSubtitleCues` 新增 `translationCues: List<SubtitleCue> = emptyList()` 参数——主句 startMs 在翻译句 ±300ms（`SUBTITLE_TRACK_ALIGN_TOLERANCE_MS`）容差窗口内命中 → 该翻译写入 `LyricLine.translations`（按主句 startMs 建映射）。
- `MusicViewModel.loadVideoSubtitleLyrics`：主轨道（原生优先，`isLikelyAiSubtitleTrack` 排除）确定后，`if (isChineseSubtitleTrack(track))` 不合并；否则从轨道列表找 `trackKey != 主轨 && isChineseSubtitleTrack` 的中文轨（官方或 AI 翻译）作为翻译轨，`getSubtitleCues` 加载后一并传入。
- UI 层无需改动：`LyricLineContent` 既有逻辑自动渲染"外语原文（主文本）+ 中文翻译（translations 行，alpha 0.72）"两行；原生是中文时 translations 为空只显示中文；"显示翻译与罗马音"开关仍可关闭双语。

**O5. 非当前歌词更清晰（需求 2，`MusicPlayerVisualPolicy.kt`）**
- `resolveMusicLyricFocusStyle`：非当前行不透明度 46/30/20 → **66/52/42**，模糊半径 2/4/7dp → **1/2/4dp**（距离 1/2/更远）；当前行保持 100% alpha / 0 blur 不变（仍最清晰）。blur 关闭路径（低版本/省流）alpha 同步用新值。
- ⚠️ **已被 O7 修订**：分级样式仅作中间态，最终以 O7 的统一样式为准。

**O6. 测试同步**
- `MusicPlayerVisualPolicyTest`：距离 1 断言 (blur 2, alpha 46) → (1, 66)；距离 3 断言 (7, 20) → (4, 42)；blur 关闭断言 (0, 46) → (0, 66)。
- 新建 `SubtitleLyricsPolicyTest`（4 用例）：中文判定（zh-Hans/ai-zh/cn ✓，ja-JP/en-US ✗）、纯单语无翻译、双语合并（±300ms 内命中）、容差外不合并。
- 已验证（2026-08-25）：`./gradlew :app:compileDebugKotlin` 与上述 2 个测试类全绿。构建需 `-Pbili.miuix.source=V:/dev/miuix` 且统一 `ANDROID_HOME`/`ANDROID_SDK_ROOT`（见 §1 环境坑）。

**O7. 非当前歌词模糊统一为相邻句样式（需求 3，`MusicPlayerVisualPolicy.kt`）**
- ⚠️ **修订 O5**：`resolveMusicLyricFocusStyle` 删除距离分级（66/52/42 与 1/2/4dp），改为二元判定——`distance == 0`（当前句）→ 100% alpha / 0 blur；**其余所有行一律 66% alpha / 1dp blur**（与相邻句完全一致，不分远近）。
- 测试同步：`MusicPlayerVisualPolicyTest` 用例改名 `lyric focus keeps current line sharp and blurs every other line like neighbors`；距离 2（隔 1 句）与距离 4（更远）断言均改为 (blur 1, alpha 66)，原距离 4 断言 (4, 42) 删除，补隔 2 句用例。
- 已验证（2026-08-25）：`MusicPlayerVisualPolicyTest` + `SubtitleLyricsPolicyTest` 全绿。

---

### P. 第十轮：首页时长默认封面内无底色 + 根动态图文点击修复（2026-08-25，**未提交**）

> 目标：① 首页卡片「首页视频时长」默认改为「封面内无底色」（OVERLAY_TEXT_ONLY：时长在封面内纯文字+阴影，无底色 pill）；② 修复根动态页（主页动态 tab）点击图文无法进详情页的两个代码缺陷（opus 图文点图弹预览不进详情；正文文字区域点击成死区）。
> 涉及：`SettingsManager.kt`、`DynamicCard.kt` + 测试 `HomeSettingsMappingPolicyTest.kt`、`HomeSettingsStoreParityTest.kt`

**P1. 首页时长默认封面内无底色（需求 1，`SettingsManager.kt`）**
- `HomeSettings.homeDurationStyle` 数据类默认 `OUTSIDE_COVER` → `OVERLAY_TEXT_ONLY`。
- `mapHomeSettingsFromPreferences`：无 `KEY_HOME_DURATION_STYLE` 记录时的默认 `OUTSIDE_COVER` → `OVERLAY_TEXT_ONLY`；旧开关 `KEY_HOME_VIDEO_DURATION_BADGES_VISIBLE=false` → `HIDDEN` 的兼容分支保留不变。
- 仅影响新装/未设置用户（既有惯例）；已设置用户保留其选择；`HomeDurationStyle.fromValue` 非法枚举兜底仍 OUTSIDE_COVER（不动）。
- 测试同步：`HomeSettingsMappingPolicyTest.emptyPreferences` 断言 `OUTSIDE_COVER` → `OVERLAY_TEXT_ONLY`；`HomeSettingsStoreParityTest.home settings keep retired card surface effects disabled` 同改。

**P2. 根动态图文点击进详情（需求 2，`DynamicCard.kt`）**
- **缺陷 A（opus 图文 feed 点图弹预览不进详情）**：`DynamicCardV2` 的 opus 图片 `DrawGridV2.onImageClick` 回调调用 `resolveDynamicCardMediaAction(item, index)` **漏传 `isDetail`**（默认 true → 恒返回 PreviewImages），且回调只处理 `PreviewImages` 分支——策略（`DynamicCardClickPolicy.kt` 的 `resolveDynamicCardMediaAction`）在 feed（isDetail=false）中本应返回 `OpenDynamicDetail`。修复：仿 article 封面分支的 `when` 结构——传 `isDetail = isDetail`，补 `OpenDynamicDetail → openDynamicDetail?.invoke(action.dynamicId)` 分支。
- **缺陷 B（正文文字区域死区）**：正文 `RichTextContent(desc, onUserClick)` 未传 `onBlankTap`，`RichTextContent` 内部 `pointerInput + detectTapGestures` 消费文本区全部点击、空白处仅调 `onBlankTap?.invoke()`（null 时无动作）→ 点正文没反应。补 `onBlankTap = { openDynamicDetail?.invoke(item.id_str) }`（与 `ForwardedContent.kt` 同款写法）。
- **不动**：手势竞争（`VerticalPriorityPagerGesture` 横滑判定吞掉带微位移的点击，偶发"有时没反应"）——用户确认不改（改它风险高）。
- 测试：策略层 `DynamicCardClickPolicyTest` 已有 opus feed→详情用例，全绿；调用点无结构测试，未新增。

**P3. 验证**
- `:app:compileDebugKotlin` + `HomeSettingsMappingPolicyTest`（homeDurationStyle 用例）/`HomeSettingsStoreParityTest`（相关用例）/`DynamicCardClickPolicyTest` 全绿（2026-08-25）。
- ⚠️ 4 个既有基线失败与本轮无关（勿修，明细已并入 §4 基线失败清单）：`HomeSettingsMappingPolicyTest` ×3（I5 默认值遗留）+ `HomeSettingsStoreParityTest` ×1（showHomeUpBadges 断言）。

**P4. 他人主页私信/关注按钮右边界对齐（需求 3，`SpaceHeaderPresentationPolicy.kt` + `SpaceScreen.kt`）**
- ⚠️ **修订 O3**：按钮行不再与统计行左右等宽。用户确认规格：**仅右边界重合**——关注按钮右边界与统计行（粉丝/关注/获赞）右边界重合；关注条子宽度减半、左边界自然右移（私信圆钮随行右移）。
- 旧实现（O3）：按钮行宽度 = 统计行 `onSizeChanged` 实测像素回传（`metricsRowWidthPx.toDp()`）——首帧 0 宽跳变 + px→dp 取整误差，关注条子 210dp 过长。
- 新实现（**推导式常量，去掉测量回传**）：
  - 统计行宽 = 3×stat宽 + 2×分隔线(1dp) + 4×间距(8dp) = **250dp**（常规屏）/ **214dp**（窄屏 <360dp）；stat 列宽 72dp/60dp。
  - 关注按钮 = (统计行宽 − 40dp) / 2 = **105dp**（常规）/ **87dp**（窄屏）；40dp = 私信圆钮 32dp + 间距 8dp。
  - 按钮行总宽 = 关注按钮 + 40dp，整行右对齐（Column `horizontalAlignment = End` + 16dp 右边距）→ 右边界恒重合，无测量、无取整误差。
- `SpaceHeaderPresentationPolicy.kt` 新增纯函数：`resolveSpaceHeaderMetricStatWidthDp(screenWidthDp)` / `resolveSpaceHeaderMetricRowWidthDp(statWidthDp)` / `resolveSpaceHeaderFollowButtonWidthDp(metricRowWidthDp)` + 常量 `SPACE_HEADER_METRIC_STAT_WIDTH_DP(72)` / `SPACE_HEADER_METRIC_STAT_WIDTH_COMPACT_DP(60)` / `SPACE_HEADER_METRIC_DIVIDER_WIDTH_DP(1)` / `SPACE_HEADER_METRIC_HORIZONTAL_SPACING_DP(8)` / `SPACE_HEADER_RELATION_ACTION_FIXED_WIDTH_DP(32+8)`。
- `SpaceScreen.kt`：删除 `metricsRowWidthPx` 及 `onSizeChanged` 回传；`SpaceHeaderStat` 列宽与分隔线宽度统一走 policy 常量（单一来源）。
- 测试：`SpaceHeaderPresentationPolicyTest` +3 用例（stat 分档 `<360→60`、行宽推导 250/214、关注减半 105/87），全绿（2026-08-25）。
- 验证：`:app:compileDebugKotlin` + `SpaceHeaderPresentationPolicyTest` 通过；`SpaceScreenStructureTest` 唯一失败为既有基线 `showTabRail`（第八轮 N 节已记载，勿修）。

---

### Q. UI 性能修复批：弹幕列表 key 化 + 全屏层安全区/返回拦截 + 手势与重组热点治理（2026-08-27，提交 `40e5775`，17 文件 +313/−180）

> 目标：修复三轮代码审查发现的高/中优先级 UI 性能与交互缺陷。**无新功能、无视觉规格变化**，全部为性能/交互修复；上游合并冲突时按"文件 + 符号名"逐条对回即可。
> 涉及：`LiveChatSection.kt`、`LandscapeChatOverlay.kt`、`FullscreenPlayerOverlay.kt`、`VideoPlayerSection.kt`、`GestureLevelOverlay.kt`、`JankTracking.kt`、`DynamicCard.kt`、`DynamicScreen.kt`、`DynamicDetailScreen.kt`、`TopicDetailScreen.kt`、`SpaceScreen.kt`、`VideoCards.kt`、`DrawGrid.kt`、`HomeCategoryPage.kt`、`FollowingListScreen.kt`、`PinyinUtils.kt`、`LivePlayerControls.kt`
> 验证：`:app:compileDebugKotlin` 通过且无新增警告 + `JankTrackingStructureTest` 通过（2026-08-27）。无新增/修改测试用例。

**Q1. 直播弹幕列表 key 化（高频弹幕房间卡顿主因，`LiveChatSection.kt` / `LandscapeChatOverlay.kt`）**
- **根因**：`messages.add(item)` + `removeAt(0)` 滑窗淘汰但 `items(messages)` 无 key → 头部删除后所有元素索引前移，LazyColumn 把全部可见项当新内容，每个气泡重 UI（AnnotatedString/AsyncImage/菜单）整批重建；且每条消息一次 `animateScrollToItem` 互相取消。
- 竖屏 `LiveChatSection`：新增 `private class ChatEntry(val id: Long, val item: LiveDanmakuItem)`，`messages` 改存 `ChatEntry` + `nextMessageId` 自增；`items(messages, key = { it.id })`；自动跟随改 `scrollToItem`（瞬时）；收集侧统一过滤 `!item.isSuperChat && !shouldRenderLiveDanmaku(item.text, item.emoticonUrl)`；表情 map `DanmakuEmoticonMapper.emoticonMap` 的 `collectAsStateWithLifecycle` 从每个气泡内**上提到列表顶层**以参数传入 `ChatMessageItem`（原 200 条 = 200 份订阅，map 更新全体失效）。
- 横屏 `LandscapeChatOverlay`：同款 `LandscapeChatEntry` + key + `scrollToItem`；补 `shouldRenderLiveDanmaku` 过滤（原横屏没有该过滤，空文本会渲染孤立的「用户名: 」行并占用 50 条窗口名额）。

**Q2. 全屏播放层安全区 + 返回拦截 + 去 haze（`FullscreenPlayerOverlay.kt`）**
- **安全区避让**（原顶部/底部控制条只有硬编码 16dp padding，immersive sticky 唤出系统栏或刘海屏时按钮重叠）：顶部 Row 加 `.statusBarsPadding().displayCutoutPadding().padding(16.dp)`（横屏下状态栏 inset 表现为左右避让）；底部 Column 加 `.navigationBarsPadding().displayCutoutPadding().padding(horizontal = 16.dp)`。
- **返回键拦截**（原 `BackHandler` 无条件退全屏，弹幕设置/倍速/比例菜单开着按返回直接拆掉全屏）：`val overlaysBlockingBack = showDanmakuSettings || showSpeedMenu || showRatioMenu`；两个 BackHandler——`enabled = overlaysBlockingBack` 时按优先级关闭面板，`enabled = !overlaysBlockingBack` 时才 `onNavigateToDetail()`。
- **Seek 卡片去实时 blur**：删除 `unifiedBlur` 调用（保留 `Color.Black.copy(alpha = 0.74f)` + 白边），`GestureIndicator` 删 `hazeState` 参数；整层 `.hazeSourceCompat(overlayHazeState)` 与 `overlayHazeState` 声明、`BlurSurfaceType`/`rememberRecoverableHazeState`/`unifiedBlur`/`HazeState`/`hazeSourceCompat` import 一并删除（拖动 seek 时不再实时模糊采样整个视频层）。

**Q3. 视频播放器手势重组治理（`VideoPlayerSection.kt` + `JankTracking.kt`）**
- **删死状态 `gestureIcon`**（声明 + 4 处 `resolveGestureLevelIcon(...)` 写入 + `gestureLevelOverlayStyle` 局部量与相关 import；浮层组件内部自解析图标，此状态只写不读）——省掉亮度/音量拖动中每帧的图标解析与分配。
- **jank 追踪 effect 化**：`JankTracking.kt` 新增 `TrackJankStateFlagEffect(stateName, isActiveProvider)` / `TrackJankStateValueEffect(stateName, valueProvider)`（`LaunchedEffect` + `snapshotFlow` 订阅，读取不进组合作用域）；`VideoPlayerSection` 顶层两个 `TrackJankStateFlag/Value` 调用改用 Effect 版——手势启停写 `isGestureVisible`/`gestureMode` 时**不再触发 5000+ 行函数体整体重组**。原组合期版函数保留（其他调用方）。
- **音量 binder 缓存**：`val maxVolumeSteps = remember(audioManager) { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }`，手势 seeding 与拖动分支共用（原每个拖动事件一次 AudioManager 往返）。

**Q4. 手势档位浮层去动画重启（`GestureLevelOverlay.kt`）**
- `remember(style, kind, percent)` → `remember(style, kind, quantizedPercent)`，`quantizedPercent` 按 `GESTURE_SPEC_QUANTUM = 0.05f`（5%）量化（percent 只影响 iOS 肤色容器 alpha 渐变，量化 20 档不可感知）——拖动中不再逐帧丢弃重建 spec 对象。
- 删除 `animateFloatAsState`（tween 目标值逐帧变更 = 每帧重启动画，进度条滞后 + 动画器开销），`progress = percent.coerceIn(0f, 1f)` 直读；`GestureLevelOverlayHost` 与 `GestureLevelOverlayContent` 两处同步。

**Q5. 动态卡 DataStore 订阅上提（`DynamicCard.kt` + 4 个调用方）**
- **根因**：每张 `DynamicCardV2` 卡内各自 `SettingsManager.getDynamicImagePreviewTextVisible(context).collectAsStateWithLifecycle(...)`——瀑布流一屏 10-20 张卡 = 同一 DataStore flow 被重复订阅 N 份，设置变更时全体卡片同时重组。
- `DynamicCardV2` 新增参数 `defaultPreviewTextVisible: Boolean = true`，删除卡内 collect；卡内 5 处引用（4 处 `defaultTextVisible` + 1 处 `ForwardedContent` 的 `defaultPreviewTextVisible`）改用参数。
- 调用方接线（每屏收集一次）：`DynamicScreen.kt`（`DynamicList` 顶层，`listContext`）→ 传给 `dynamicCard` lambda；`DynamicDetailScreen.kt`（`cardContent` 定义前，composable 作用域）；`TopicDetailScreen.kt`（`else ->` 分支、LazyColumn **外**）；`SpaceScreen.kt`（`dynamicCardItems` 附近收集 → `SpaceContent` 新增同名参数 → 动态 tab 调用点传入）。
- ⚠️ **作用域坑**：`collectAsStateWithLifecycle` 是 @Composable，不能放进 LazyColumn/LazyVerticalGrid 的 LazyListScope builder lambda 内（非组合作用域，编译报 "invocations can only happen from @Composable context"）——必须放在 builder 之外的组合作用域。

**Q6. 动态媒体卡坐标惰性求值（`VideoCards.kt` / `DrawGrid.kt`，对齐首页 `VideoCard` 既有做法）**
- **根因**：`onGloballyPositioned { ref.value = coordinates.boundsInRoot() }` 在滚动期间每帧做坐标树遍历 + Rect 分配（首页卡片注释里明确规避的反模式，动态侧回退了）。
- `VideoCardLarge`：`cardBoundsRef/coverBoundsRef`（存 Rect）→ `cardCoordsRef/coverCoordsRef`（存 `LayoutCoordinates`）；`onGloballyPositioned` 只存引用；`triggerClick` 内 `takeIf { it.isAttached }?.boundsInRoot()` 惰性求值。
- `DrawGridImage`：`var imageRect by remember` → `imageCoordsRef`；点击回调内 `boundsInWindow()`；新增 `import androidx.compose.ui.layout.LayoutCoordinates`。

**Q7. 推荐页组合期 O(n) 缓存（`HomeCategoryPage.kt`）**
- **根因**：轮播筛选 `selectHomeHeroCarouselItems` / 排除过滤 `excludeHomeHeroCarouselItems` / 锚点扫描 `any{}` / `resolveHomeCategoryVideoGridKeys` 全量 key 构建都无 remember——任何无关状态（`dissolvingVideos`/`followingMids` 等）变化引发重组时对数百条视频重复全量计算。
- 全部 remember 化并**上提到 `LazyVerticalGrid` 之外的组合作用域**（`carouselVideos` / `showHeroCarousel` / `visibleGridVideos` / `videoGridKeys` / `oldContentDividerIndexes`）；⚠️ 同 Q5 作用域坑——LazyGridScope builder 内不能调 remember。
- 分割线插入位收敛为 `oldContentDividerIndexes: Set<Int>`（buildSet）：锚点命中 → 所有 `index > 0` 的锚点位置；锚点为空 → `startIndex`（>0 且 <size）；**语义显式化：锚点存在但未命中时不回退 startIndex 分支**（与原短路逻辑等价）。
- `forEachIndexed + item()` 结构保留（分割线与卡片交错，改 `items(count)` 需重排有回归风险，收益次要）。

**Q8. 关注列表搜索去抖 + 后台拼音过滤（`FollowingListScreen.kt` + `PinyinUtils.kt`）**
- **根因**：`remember(usersByGroup, searchQuery) { usersByGroup.filter { PinyinUtils.matches(...) } }` 在组合期同步执行——该页特意后台拉全量关注（为了全量搜索），大用户量时每个按键都在主线程对全部 UP 做拼音转换与匹配。
- `PinyinUtils` 新增 `matchesCached`（`ConcurrentHashMap` 的 `pinyinCache`，同文本拼音只转换一次，线程安全）；原 `matches` 保留未动。
- 过滤链路重写：150ms 去抖（`FOLLOW_SEARCH_DEBOUNCE_MS = 150L`，`debouncedSearchQuery`）→ `produceState(initialValue = usersByGroup, usersByGroup, debouncedSearchQuery)` → 空查询恒等返回，非空 `withContext(Dispatchers.Default)` 内 `matchesCached` 过滤。
- 顺带修 `AnimatedBlurFadeText` 逐帧重组：`.alpha(alphaAnim.value).blur(blurAnim.value.dp)`（组合期读动画值）→ `graphicsLayer { alpha = ...; renderEffect = BlurEffect(radiusPx, radiusPx) }` 绘制期读取（API < S 上 Compose BlurEffect 自动降级为仅透明度）；新增 import `BlurEffect`/`graphicsLayer`。

**Q9. 直播控制条手势（`LivePlayerControls.kt`）**
- **尺寸实时读取**（修复旋转/全屏切换后三分区与灵敏度全错）：原 `pointerInput(Unit)` 启动时一次性捕获 `size.width/height`（Unit key 不随尺寸变化重启）→ 删除顶层捕获，`onDragStart` 内读 `size.width`（分区判定）、`onDragEnd`/`onVerticalDrag` 内读 `size.height`（切换阈值/灵敏度）。
- **亮度节流**：`LIVE_BRIGHTNESS_WRITE_THRESHOLD = 0.02f`（文件级常量），`lastAppliedBrightness` 跟踪，超阈值才写 `window.attributes`（对齐视频侧；原每拖动事件一次 WindowManager 事务）。
- **音量节流**：`lastSetVolume` 本地跟踪替代每事件一次 `getStreamVolume` binder 读；仅档位变化才 `setStreamVolume`。
- 死状态 `gestureIcon` 删除（浮层 `GestureLevelOverlayContent` 内部自解析图标，与 Q3 同病）。

**Q10. 与其他节的边界**
- 本批全部为修复，**不改任何默认值/视觉规格**；与 P2（动态图文点击）、N12（showActionBar）等节改动点不重叠。
- 上游若带回"卡片内直接 collect 设置 flow / 无 key 的聊天滑窗列表 / 拖动手势在组合期读写瞬态状态 / onGloballyPositioned 内立即 boundsInRoot()"等模式，按本节口径重新应用。

---

### R. 性能轮：滑动/加载/过渡稳帧 + 后台降耗 + 听视频锁屏误暂停修复（2026-08-27，提交 `d1a2ed7` `a872da3` `80418d5` `3c66eaa` `fc14a2f` `3efc07c`）

> 原则：**零视觉损失**（只减不必要开销，不改 1080P@90、不改视觉规格）；每阶段 `:app:compileDebugKotlin` + 相关单测通过后独立提交。上游冲突按"文件 + 符号名"对回；行号仅定位参考。

**R1. 滑动稳帧（`d1a2ed7` + `a872da3`）**
1. 首页卡片挂载期同步磁盘 IO（`d1a2ed7`）：`PlaybackProgressManager` 缓存上限 100 → 512、`getCachedPosition` 去掉同步 SharedPreferences 回退；`PlayerSettingsCache` 新增点击播放镜像（存储键 `auto_play_cache`，`setClickToPlay`/`getClickToPlay` 走缓存）；`SettingsManager.getClickToPlaySync` 委托缓存；启动任务 `playback_progress_prewarm`（AppScope.ioScope）预热进度缓存。
2. 故事卡坐标引用化：`StoryVideoCard` 的 `cardBounds/coverBounds`（State，每帧 boundsInRoot 写入）→ 非 State `cardBoundsRef/coverBoundsRef`，点击回调内惰性求值（对齐 Q6 口径）。
3. `VideoCardOnlineCountStore`：请求接口加 `isScrollInProgress: Boolean = false` 参数，滚动期跳过请求。
4. 首页轮播：`HomeHeroCarousel` `transformProvider` 改 lambda 绘制期读页面状态（不进组合）；`zIndex` 量化为中心页 1f/其余 0.99f；封面阴影并入 `graphicsLayer`、按 1dp 步进量化。
5. 底部页容器预挂载：±1 页且 idle 时才挂载（`(pageCount-1).coerceAtMost(1)`），但**导航转场中（isNavigating）全量挂载**保证 morph；同步更新 4 个锚点结构测试。
6. 抖动特效 `jiggleOnDissolve`：动画驱动改 `DrawModifierNode`（`JiggleElement/JiggleNode`）——`draw()` 内直读全局状态 + `System.nanoTime` sin 波形（160ms 旋转 ±1.5° / 120ms 位移 ±1dp，Canvas save/rotate/translate 包 drawContent）+ `invalidateDraw` 自驱，不再逐帧重组。

**R2. 加载提速（`80418d5`）**
1. 首页封面预加载对齐真实请求：复用 `homeCoverRequestSpec.resolveUrl` + `resolveVideoCardCoverCacheKey`（同 key 同尺寸 → 内存缓存命中）。
2. `HomeViewModel` 删除 3 处 `delay(100)` 人工节流。
3. `fetchWebFeed` 删除冗余第二次 `getNavInfo`（失败快速抛出）；新增启动任务 `wbi_key_prefresh`（15s 延迟，ioScope，`shouldPrefresh` 过期门控）空闲预刷新 WBI。
4. Coil 内存缓存占比 `resolveImageMemoryCachePercent` 0.10 → 0.18。
- 有意跳过：feed 本地缓存（2.5）、推荐页页大小 20→30（2.6）——上游更新时**不要补做**。

**R3. 过渡动画稳帧（`3c66eaa`）**
1. 视频加载/评论初始化等重活 LaunchedEffect 由既有 `isTransitionFinished` 门控（morph 落定前不启动）。
2. 壁纸层删除与转场效果重复的 `videoCardTransitionBackgroundEffect`（参数一并移除，`AppNavigation` 调用点同步简化）。
3. `animateFallbackTo`：存在活动 morph 进度时立即 snap；Host 返回分支改 `snapshotFlow{}.first{...}` 等待后再 `snapFallback(0)+markIdle`（消除返回动画竞态）。
4. `MiuixVideoCardClipShape.fromRadii`：圆角 2px 量化 + `LruCache(16)`（morph 中逐帧 Shape 分配消除）。
5. 预测性返回背景：MIUIX 风格下传 `skipBlurRenderEffect=true`（该风格本就无模糊渲染）。
6. 骨架屏脉冲在转场中归零：新增 `LocalVideoCardTransitionInProgress`（`phase==OPENING || RETURNING` 时 true），两处 skeleton 脉冲振幅读它返回 0f。

**R4. 弹幕引擎阶段 4：分析后整体跳过（重要口径，勿复做）**
- **4.1 GPU 纹理化不做**：计划前提"每帧 CPU Canvas 软绘"错误——`DanmakuRenderView`(FrameLayout) → `DanmakuView : View` 是**硬件加速 Canvas**（RenderThread GL 后端），drawText 走 Skia GPU 路径且有 glyph atlas 缓存；纹理化收益存疑（只剩 DisplayList 录制开销）却引入位图生命周期/失效 key/命中测试全套复杂度，回归只能实机目测。**若实机 gfxinfo 对照（弹幕开关×密度）证实弹幕为瓶颈再议。**
- **4.3 VOD trimBefore 不需要**：VOD 本就分段窗口加载（`DanmakuManager` 4 处 `shouldReplaceDanmakuWindow` + `DanmakuSegmentWindowPolicy`），`currentItems` 只持活跃段；`ByteDanceDanmakuEngine.append` 已有有序快速路径；`trimBefore` 已实现且直播在用（`LiveDanmakuOverlay`）。
- **4.2 每帧临时 list+sort**（`RenderEngine.draw` L110 附近）真实但微秒级（`wrapData` 已有对象池），低优先级可顺手做。

**R5. 后台降耗（`fc14a2f`，全部无观感影响）**
1. `AppNavigation` 动态未读轮询：`if (!BackgroundManager.isInBackground)` 门控（仓库既有生命周期信号，未引 repeatOnLifecycle）。
2. `ImmersiveStatusBarBackdrop` 环境采样 `VIDEO_STATUS_BAR_AMBIENT_CAPTURE_INTERVAL_MS` 66L → **200L**（重度模糊下的低频视觉信号）；测试断言改 `150L..300L`。
3. `BangumiPlayerComponents` 两处 200ms 轮询（主状态 ~L328 / 迷你进度条 ~L817）：暂停且非拖动时 `delay(1_000)` 保底同步，播放/拖动自动回 200ms。
4. `VideoPlayerSection` 字幕位置 `produceState` 事件驱动：暂停期**完全退出 tick**，`Player.Listener`（`onIsPlayingChanged`/`onPositionDiscontinuity`）+ `snapshotFlow{...}.first{...}` 唤醒；播放中保留 120ms。⚠️ 字幕 cue 是本地数组（`resolveSubtitleTextAt`）非 Media3 文本轨道，计划中的 `onCues` 对该链路不触发，勿按原计划复刻。
5. seek 会话同步循环：仅会话活跃（`session.isSliderMoving || session.pendingSeekPositionMs != null`）时 200ms 同步，否则 600ms 纯挂起（原常驻 200ms `player.currentPosition` 轮询）。
6. Overlay 隐藏档放宽（5.6）：`resolveFullscreenPlayerPollingIntervalMs` 隐藏档 400/800 → **1000/2000**ms；`resolveInlineVideoOverlayProgressPollingIntervalMs` 隐藏档 500 → **1000**ms。安全性依据：可见性切换即 effect key，重启首迭代立即全量同步 → 零视觉滞后。测试同步：`ImmersiveStatusBarBackdropPolicyTest` / `FullscreenPlayerOverlayPollingPolicyTest` / `VideoPlayerOverlayPolicyTest`。

**R6. 听视频页锁屏误暂停修复（`3efc07c`，bugfix）**
- **症状**：设置已开「后台播放」，听视频 tab 页播放视频 → 锁屏自动暂停。
- **根因**：`AudioModeScreen` 在无活跃视频会话时走 `shouldCreateAudioModeStandalonePlayer` → `rememberVideoPlayerState` 创建**独立 player**，该 player 不归 `MiniPlayerManager` 会话管理；锁屏触发 `VideoPlayerState` ON_PAUSE observer 时 `isBackgroundAudio = miniPlayerManager.shouldContinueBackgroundAudio()` 因 `isActive=false` 返回 false，`isMiniMode/isPip` 也为 false → 三条件全 false → `player.pause()`。设置开关根本不在此判定链上。
- **修复**（Policy 参数透传模式）：`PlaybackLifecycleCoordinator` 的 `resolvePlaybackPauseDecision`/`resolveShouldContinuePlaybackDuringPause` 新增 `isAudioOnlySession: Boolean = false`（与 mini/PiP 同级放行）；`rememberVideoPlayerState` 新增 `audioOnlySession = false` 参数并透传至 ON_PAUSE 决策；`AudioModeScreen` standalone 创建点传 `true`。测试：`PlaybackLifecycleCoordinatorTest` 新增 `pauseDecision_keepsPlayingForAudioOnlySessionEvenWithoutManagedBackground`。
- **边界**：视频详情页调用点默认 false 行为不变；`viewModel.currentPlayer`（PLAYER_VIEW_MODEL）路径无 observer 附着，不受影响。上游若重构 ON_PAUSE 决策，**必须保留纯音频会话放行分支**。

---

### S. UI 细节一致性整改批（2026-08-28，提交 `3058da1` `f25a75e` `8e1accf` `795897c`，15 文件 +1 新建）

> 目标：消除"同一 App 内多套视觉标准并行"造成的廉价感。首轮只做观感收益最高的第一梯队 4 项：① 信息流卡片圆角统一 ② 等级徽章配色单一来源 ③ 清除两个非 App 风格弹窗 ④ 播放器菜单主题化。**纯视觉一致性，无逻辑改动、无新依赖**；上游合并冲突时按"文件 + 符号名"对回。
> 涉及：`feature/home/components/cards/`（5 张卡）、`HomeFeedSkeletonCard.kt`、`SpaceScreen.kt`、`core/ui/components/`（新建 `BadgeLevelPalette.kt` + 改 `UserLevelBadge.kt`）、`ReplyComponents.kt`、`DynamicCreateReserveDialog.kt`、`SearchVideoFilterSheet.kt`、`QualityMenu.kt`、`AudioQualitySelectionMenu.kt`、`BangumiPlayerComponents.kt` + 测试 `HomeFeedSkeletonCardStructureTest.kt`
> 验证：`:app:compileDebugKotlin` + `HomeFeedSkeletonCardStructureTest` / `StoryVideoCardStructureTest` / `VideoCardScrollLiteVisualPolicyTest` / `HardcodedColorMigrationGuardTest` 全绿（2026-08-28）。

**S1. 信息流卡片圆角统一到 AppShapes Card 级（`3058da1`，8 文件）**
- **问题**：同一信息流四张卡片四种圆角（VideoCard 8dp、CinematicVideoCard 16dp、Glass/Story 卡 20dp、LiveRoomCard 14dp），混排时观感像"换样式=换 App"；且 VideoCard/CinematicVideoCard 用**间距 token 当圆角**（`AppSpacingTokens.Small/Large * LocalCornerRadiusScale`），绕过 AppShapes 语义层。LiveRoomCard 是首页唯一带阴影的卡片（elevation 也是间距 token），混排中"浮起来"。
- **统一改法**：全部 `AppShapes.containerCornerDp(ContainerLevel.Card)`（基准 12dp，随皮肤缩放 MIUIX ×1.15 / MD3 ×0.9，`AppShapes` 内部自处理，无需再乘 LocalCornerRadiusScale）：
  - `VideoCard.kt`：`AppSpacingTokens.Small * cornerRadiusScale` → Card，删 `LocalCornerRadiusScale` import
  - `CinematicVideoCard.kt`：`AppSpacingTokens.Large * cornerRadiusScale` → Card，删同名 import、补 `AppShapes`/`ContainerLevel` import
  - `GlassVideoCard.kt`：壳 `ContainerLevel.Sheet`(20) → `Card`；封面内嵌圆角 `Dialog + Micro` → `cardCornerRadius - AppSpacingTokens.Micro`（保持"内层 < 外层"嵌套规则）
  - `StoryVideoCard.kt`：`Sheet`(20) → `Card`（`val coverShape = RoundedCornerShape(cardCornerRadius)` 行保留，`StoryVideoCardStructureTest` 断言它）
  - `LiveRoomCard.kt`：`Dialog`(14) → `Card`；**删除 `.shadow(...)`** 整块 → `.clip(RoundedCornerShape(cardCornerRadius))`，删 `androidx.compose.ui.draw.shadow` import（`MediaContrastPalette` 其他用途保留）
  - `HomeFeedSkeletonCard.kt`：骨架卡同款 8dp → Card（骨架与真实卡必须同圆角，否则加载完成瞬间圆角跳变）
  - `SpaceScreen.kt`：他人主页投稿网格 `spaceFeedCornerRadius` 同款 8dp → Card
- **测试同步**：`HomeFeedSkeletonCardStructureTest.featureSkeletonUsesSharedHomeGeometryAndReversePulse` 断言 `cardCornerRadius = AppSpacingTokens.Small * LocalCornerRadiusScale.current` → `cardCornerRadius = AppShapes.containerCornerDp(ContainerLevel.Card)`。
- ⚠️ **结构测试是源码字符串断言**：`VideoCardScrollLiteVisualPolicyTest` 读取 Story/Glass 卡源码中 `cardShellShape = remember(cardCornerRadius)` 行——只改圆角来源行，勿动这些行；`LiveRoomCard.kt` 在 `HardcodedColorMigrationGuardTest` 监控名单（禁新增 `Color(0x…)` 字面量），本轮只删不增。
- **明确未做**：直播卡 aspect ratio 16:10 vs 视频卡 16:9（后续轮候选）。

**S2. 等级徽章配色收敛到 BadgeLevelPalette（`f25a75e`，3 文件）**
- **问题**：同类徽章各写各的硬编码色阶——评论区粉丝勋章 `ReplyComponents.resolveFansMedalColor`（阈值 30/20/10：E67A2B/D9963B/CFA657/B6B6B6）与用户 LV 徽章 `UserLevelBadge.resolveUserLevelFallbackColor`（阈值 6~2：F04444/FF7A45/FF8B5A/FF9C6E/FFAE84/A7ADB8）两套 ramp 并行且不随主题。
- **改法**：新建 `core/ui/components/BadgeLevelPalette.kt`（与 UserLevelBadge 同包）：`userLevel(level)` + `fansMedal(level)` 两个纯函数，**数值原样搬迁（纯收口不调色）**；`UserLevelBadge.kt` 与 `ReplyComponents.kt` 删本地 ramp 改引用。
- **不动**：直播间 `LiveChatSection` UL 徽章已走 `LiveBiliPaiVisualPolicy.resolveLiveLevelColor`（LiveStatusPalette），本就集中，勿重复迁移。

**S3. 弹窗体系统一收口（`8e1accf`，2 文件）**
- **问题**：全 App 200+ 处弹窗已走 `AppAlertDialog`/`AppModalBottomSheet` 封装，唯二漏网：① `DynamicCreateReserveDialog.kt` 直接用 `android.app.DatePickerDialog`+`TimePickerDialog`（系统 stock 样式，同屏反差最大）；② `SearchVideoFilterSheet.kt` 用 material3 `DatePickerDialog`（按钮/圆角与 App 弹窗体系不同源）。
- `DynamicCreateReserveDialog`：平台对话框删除；"开播时间"按钮改为弹 `ReserveDatePickerDialog`（AppAlertDialog + material3 `DatePicker`，确认键"下一步"）→ 串联 `ReserveTimePickerDialog`（AppAlertDialog + material3 `TimePicker`，`is24Hour = true`），确认后按 `pickedDateMillis` + hour/minute 重组 `startAtMillis`（秒/毫秒归零逻辑保留）；`LocalContext`/`android.app.*` import 删除。
- `SearchVideoFilterSheet.SearchDatePickerDialog`：容器 material3 `DatePickerDialog` → `AppAlertDialog`（`text` 槽放 `DatePicker`，按钮槽 `AppDialogAction`），`rememberDatePickerState` 不变；删 `androidx.compose.material3.DatePickerDialog` import。

**S4. 播放器菜单主题化（`795897c`，3 文件）**
- **问题**：画质/倍速/音质菜单容器写死 `Color(0xFF2B2B2B)` + `Color.White` 文字 = 暗色-only 组件，浅色主题下是突兀黑块。共 4 处容器（`QualityMenu.kt` 画质菜单 + 倍速菜单、`AudioQualitySelectionMenu.kt`、`BangumiPlayerComponents.kt` 番剧画质菜单）。
- **统一映射**：容器 → `colorScheme.surfaceContainerHigh`；标题/行文字 `Color.White(.copy(0.9))` → `onSurface`；禁用态 `Color.White.copy(0.4f)` → `onSurface.copy(alpha = 0.38f)`（M3 禁用口径）；分隔线 `Color.White.copy(0.1f)` → `outlineVariant`；`QualityMenu` 非 VIP tag 底 `Color(0xFF666666)` → `surfaceContainerHighest`。
- **不动**：黑 scrim（`Color.Black.copy(0.5f)` 浮层遮罩）、彩色底上的白字（primary/会员粉 tag 文字）、primary 选中态与勾选图标。

**S5. 与其他节的边界 / 未做项（后续轮候选）**
- 本批与 Q/R 节改动点不重叠；`HomeFeedSkeletonCardStructureTest` 断言已随本轮更新（**非基线失败**，应全绿）。
- 未做（按观感收益排序的后续梯队）：②空/错误状态统一接 `AppContentState`（现 59 文件自写占位）、分隔线浓度统一（38 处 ad-hoc alpha）、字号硬编码 ~770 处迁移、`VideoInfoSection` 排版 token 化、暗色-only 组件清剩余（SeekPreviewBubble 等）、直播标识红统一；操作逻辑 iOS 化（push 转场全面接入/侧滑返回）另立轮次。

---

## 3. 卡顿根因分析（备忘，供排查）

| 机制 | 证据位置 | 影响 |
|---|---|---|
| 响应式读 `isScrollInProgress` 进 composition → 滚动起/停各一次全量重组风暴（20-30 张卡、各页头栏/底栏） | `HomeCategoryPage.kt:393`、`WatchLaterScreen.kt`、`CommonListScreen.kt`、`PartitionScreen.kt`、`DynamicTopBar.kt` | 主因（已修） |
| 每卡结构性增删 `drawBackdrop` 液态玻璃 → 停止瞬间 20-30 卡同时重建 shader/delegate | `VideoCard.kt:1306` `!scrollLiteModeEnabled` | 主因（已修） |
| `unifiedBlur` 边界切 `blurEnabled` + `inputScale` → Haze delegate 切换（`RenderEffectBlur`↔`ScrimBlur` = attach/detach + inputScalePolicy.reset + renderEffectCache.clear），全屏 backdrop 重捕获 | `UnifiedBlur.kt:119-134` | 主因（已修） |
| 次生：`RuntimeVisualGuard` 检测掉帧后降级 | `RuntimeVisualGuardTracker.kt` | 放大器 |

**Haze 关键机制**：`blurEnabled=false` 不是"跳过模糊"，而是切换整个渲染 delegate（`RenderEffectBlurVisualEffectDelegate`↔`ScrimBlurVisualEffectDelegate`）；官方基准刻意剔除 `effectAttach` 耗时 → delegate 切换是主要性能杀手。

---

## 4. 测试

### 关键测试（新版本应用后应全绿）
```bash
.\gradlew :app:testDebugUnitTest --tests "com.android.purebilibili.feature.video.ui.components.VideoAspectRatioLayoutPolicyTest" --tests "com.android.purebilibili.feature.video.screen.VideoDetailLayoutModePolicyTest" --tests "com.android.purebilibili.feature.video.ui.section.VideoPlayerSectionPolicyTest" --tests "com.android.purebilibili.feature.home.components.HomeHeaderVisualPolicyTest" --tests "com.android.purebilibili.core.ui.performance.AppDisplayModePolicyTest"
```
另：`HomeScrollCoordinatorTest` 应全过（滚动性能回归守门）。

第三轮（I 节）相关测试：
```bash
# design-system 模糊/主题默认
.\gradlew :design-system:testDebugUnitTest --tests "com.android.purebilibili.core.ui.AppThemeConfigTest" --tests "com.android.purebilibili.core.ui.blur.BlurIntensityVisualPolicyTest" --tests "com.android.purebilibili.core.ui.blur.BlurBudgetPolicyTest"
# app 模糊偏好
.\gradlew :app:testDebugUnitTest --tests "com.android.purebilibili.core.ui.blur.UnifiedBlurPreferencePolicyTest"
```

第四轮（J 节）相关测试（29 用例全绿）：
```bash
.\gradlew :app:testDebugUnitTest --tests "com.android.purebilibili.feature.video.danmaku.DanmakuSyncPolicyTest" --tests "com.android.purebilibili.feature.video.danmaku.DanmakuPlaybackSyncPolicyTest"
```

第六轮（L 节）相关测试（全绿）：
```bash
.\gradlew :app:testDebugUnitTest --tests "com.android.purebilibili.feature.list.CommonListHeaderCollapsePolicyTest" --tests "com.android.purebilibili.feature.list.CommonListAppearancePolicyTest" --tests "com.android.purebilibili.feature.list.PersonalListSelectorStructureTest"
```

第九轮（O 节）相关测试（全绿）：
```bash
.\gradlew :app:testDebugUnitTest --tests "com.android.purebilibili.feature.audio.screen.MusicPlayerVisualPolicyTest" --tests "com.android.purebilibili.feature.audio.lyrics.SubtitleLyricsPolicyTest"
```

性能轮 R5/R6（08-27）相关测试（全绿）：
```bash
.\gradlew :app:testDebugUnitTest --tests "com.android.purebilibili.feature.video.ui.overlay.ImmersiveStatusBarBackdropPolicyTest" --tests "com.android.purebilibili.feature.video.ui.overlay.FullscreenPlayerOverlayPollingPolicyTest" --tests "com.android.purebilibili.feature.video.ui.overlay.VideoPlayerOverlayPolicyTest" --tests "com.android.purebilibili.feature.video.playback.session.PlaybackLifecycleCoordinatorTest"
```

S 轮（UI 细节一致性，08-28）相关测试（全绿；含 1 个随本轮更新的断言，非基线失败）：
```bash
.\gradlew :app:testDebugUnitTest --tests "com.android.purebilibili.feature.home.HomeFeedSkeletonCardStructureTest" --tests "com.android.purebilibili.feature.home.components.cards.StoryVideoCardStructureTest" --tests "com.android.purebilibili.feature.home.components.cards.VideoCardScrollLiteVisualPolicyTest" --tests "com.android.purebilibili.core.theme.HardcodedColorMigrationGuardTest"
```

### 基线失败清单（既有，非本次改动引入，**不要试图修复**）
| 测试 | 失败原因 |
|---|---|
| `VideoDetailScreenPolicyTest` ×4 | ① `resolveVideoPlayerSectionTarget` 含 `normalizedRouteCover.isNotEmpty()` 回退；② 同上；③ 缺 `LocalSharedTransitionEnabled`；④ 工作区 CRLF 致 `\n` 多行模式不匹配（原始 blob 是 LF，`core.autocrlf=true` 无 `.gitattributes`） |
| `HomeHeaderVisualPolicyTest` ×11、`BottomBarSurfaceColorPolicyTest` ×1 | 既有基线（**已 stash 验证**：第三轮改动前后失败集完全一致，与 I 节无关） |
| `SearchScreenPolicyTest` ×1 | `horizontalScroll(rememberScrollState())` 断言 vs 代码 `horizontalScroll(scrollState)` |
| `PagerIndicatorContinuityStructureTest` ×2、`PlaybackCoordinatorTest` ×1、`AudioModeAudioQualityStructureTest` ×1、`BottomBarLiquidSegmentedControlStructureTest` ×1、`VideoDetailReturnCoverPolicyTest` ×2、`VerticalPriorityPagerGestureTest` ×1 | 既有基线 |
| `SpaceScreenStructureTest` ×2 | 既有基线（**已 stash 验证**：HEAD 即失败）——`showTabRail = false` 与 `onPrimaryClickOverride = { onSpaceDynamicCommentClick(dynamic) }` 在当前代码中已不存在，与 M 节无关 |
| `HomeSettingsMappingPolicyTest` ×3 | 第三轮 I5 默认值遗留：`emptyPreferences` isHeaderBlurEnabled、`invalidHomeHeaderCollapseModeFallsBackToBoth` collapseMode BOTH、`legacyAndroidNativeTopTabLiquidGlassKey_mapsToGlobalOptIn` liquidGlass（**已 stash 验证**：改动前后失败集一致） |
| `HomeSettingsStoreParityTest` ×1 | `home settings keep retired card surface effects disabled` showHomeUpBadges 断言（第三轮 I5 遗留） |
| `MusicPlayerLayoutPolicyTest` ×1 | `compact artwork`：断言 320 vs 实现 288（HEAD 即失败） |
| settings-core 单测编译 | kotlin-test 1.9.22 与 Kotlin 2.x 不兼容（模块配置问题，与本轮改动无关） |
| `OpenSourceLicensesPolicyTest` / `MiuixV2MigrationStructureTest` / `AnimationSettingsScreenStructureTest` / `SettingsLocalBackHandlerStructureTest` 等结构快照测试 | 既有基线（检查旧文案/构建脚本，与设置页当前实现漂移） |

---

## 5. 构建 / 签名 / 交付 / 验证流程

### 环境变量
```cmd
set JAVA_HOME=V:\dev\jdk\jdk-21.0.12+8
set ANDROID_HOME=V:\dev\android-sdk
```

### 编译验证
```cmd
V:\dev\gradle-dist\gradle-9.5.0\bin\gradle.bat :app:compileDebugKotlin ^
  --max-workers=2 ^
  -Dorg.gradle.configuration-cache.parallel=false ^
  "-Pbili.miuix.source=V:/dev/miuix" ^
  --console=plain
```

### 构建 release
```cmd
V:\dev\gradle-dist\gradle-9.5.0\bin\gradle.bat :app:assembleRelease ^
  --max-workers=2 ^
  -Dorg.gradle.configuration-cache.parallel=false ^
  "-Pbili.miuix.source=V:/dev/miuix" ^
  --console=plain
```
产物：`app\build\outputs\bilipai\release\BiliPai-<版本>.apk`（debug 包为 `:app:assembleDebug` → `BiliPai-<版本>-debug.apk`）

### 签名（release 无签名配置，必须签名）
```cmd
V:\dev\android-sdk\build-tools\37.0.0\apksigner.bat sign ^
  --ks %USERPROFILE%\.android\debug.keystore ^
  --ks-pass pass:android ^
  --ks-key-alias androiddebugkey ^
  --key-pass pass:android ^
  --out <输出路径> <输入apk>
```

### 复制到桌面交付目录
```cmd
copy <签名后apk> "%USERPROFILE%\Desktop\新建文件夹 (2)\"
```

### 验证签名
```cmd
V:\dev\android-sdk\build-tools\37.0.0\apksigner.bat verify --print-certs <apk路径>
```
期望：`V3.0 Signer: certificate DN: C=US, O=Android, CN=Android Debug`

### 真机验证清单
1. `:app:compileDebugKotlin` BUILD SUCCESSFUL
2. 关键测试全绿（见第 4 节）
3. `adb install` 成功；`adb shell dumpsys display` 确认 1080P@90
4. 竖屏详情页：播放器区域 = 状态栏下 → 屏高 70%；视频 FIT 居中（矮宽视频上下等高黑边 / 高窄视频左右等宽黑边），无拉伸
5. 竖屏详情页上滑：播放器下沿跟手上移（body 任意位置），缩到 16:9 停止，下滑恢复；黑边随比例平滑演化（上下 → 无 → 左右）
6. 横屏视频详情页：播放器仍 16:9，上滑不联动（滚动直接走内容）
7. tab 行（简介/评论 + 弹幕三件套）恒全高固定、跟播放器下沿；下沿 1dp 浅灰分割线始终可见；弹幕覆盖整个播放器区域（含黑边）
8. 全屏按钮：竖屏视频点击无任何反应
9. 新装/清数据：默认 Material3（materialYou 跟随壁纸）、无任何模糊、首页顶栏始终显示（滚动不折叠）；设置页主题/模糊开关仍可手动开启
10. 首页顶部：实色底纹，下沿比标签 dock 再低 13dp；搜索胶囊/头像/按钮无边框
11. 弹幕保留（第四轮 J 节）：暂停 → 播放，屏幕上弹幕从冻结处继续滚动、**不清空**；连续播放 ≥1 分钟（跨越 2~3 个漂移周期）不再周期性清空；缓冲恢复（网络抖动/切清晰度）弹幕原地继续
12. 弹幕清空仍生效的场景（预期）：拖进度条/跳转、切集、关闭弹幕开关、修改弹幕样式设置——时间线跳变应清屏重建
13. 弹幕设置面板：基础区（字体/透明度/速度）+ 高级渲染区（**字体粗细** 1~9、描边粗细、行高等）均可调并实时生效
14. 切 tab 保持播放器大小（第五轮 K 节）：竖屏视频上滑把播放器缩到中间任意位置 → 切到「评论」再切回「简介」→ **播放器保持缩小后的大小，不弹回 70%**；一键回顶仍恢复
15. PiP 宽高比（第五轮 K 节）：竖屏视频按 Home → PiP 窗口为竖条（9:16）；横屏视频按 Home → PiP 窗口为 16:9；PiP 恢复全屏后播放正常。若设备最近任务看不到 BiliPai（问题 2）→ 先确认「画中画」权限已开启、后台播放模式为「画中画」（ROM 行为，代码无移除逻辑）
16. 稍后再看（第六轮 L 节）：页面上方无「全部/未看完」tab；底纹范围 = 标题栏 + 搜索框（不再含 tab 行）；第一张卡片距底纹下沿约 8dp
17. 我的收藏（第六轮 L 节）：无「视频/番剧/影视/文章/笔记/话题/课程」分类标签；卡片整体上移（首卡距底纹下沿约 8dp）；列表上下滚动时**搜索框始终固定不随列表移动**；底纹透明度/颜色与稍后再看一致（模糊关 = 全透明，模糊开 = 毛玻璃半透明），搜索框底色与稍后再看一致
18. 其他用户主页（第七轮 M 节）：头部 = banner 图片 + 头像左下（2dp surface 圆环，伸入 banner 20dp、伸出 60dp）+ 右下栏（粉丝/关注/获赞数字 14sp + 私信/关注按钮）；名字/等级/VIP 徽标独占一行在 banner 下方；滚动折叠/吸顶 tab 不变
19. 本人主页（第七轮 M 节）：无 硬币/经验/经验条/编辑资料；快捷入口（离线缓存/历史/订阅/稍后再看）+ 收藏夹 + tabs + 投稿预览**整体上移且无圆角底纹**（内容平铺在页面背景上，无顶边线段/阴影线段）
20. 直播弹幕（Q1）：高频弹幕房间滚动流畅无整屏闪重；横屏弹幕浮层无孤立「用户名: 」空行；用户长按菜单/表情图正常
21. 全屏播放（Q2）：唤出系统栏时顶部返回/时间与底部控制条不与系统栏/刘海重叠；弹幕设置/倍速/比例面板打开时按返回 = 关面板，再按返回才退出全屏；seek 拖动手势正常（seek 卡片为纯黑底白边，无毛玻璃）
22. 直播手势（Q9）：进直播间 → 切全屏/旋转后，左 1/3 亮度、右 1/3 音量、中间上滑切全屏的分区与灵敏度仍正确；亮度拖动平滑
23. 动态/推荐（Q5/Q6/Q7）：九宫格图片点击预览展开动画起点正确（坐标惰性求值未破坏传 rect）；推荐流滚动与轮播/今日观看/分割线显示正常；动态卡「展开文案」设置开关仍实时生效
24. 关注列表（Q8）：搜索输入流畅（快速连打无明显卡顿）、中文/拼音/首字母均能命中；顶部统计文案切换动画正常
25. 听视频锁屏（R6）：① 底部 tab 直进听视频页选视频播放 → 锁屏 → **继续播放不暂停**，解锁回前台不跳进度、不重复起播；② 视频详情页正常锁屏行为（后台播放开启时继续播）不受影响
26. 后台/暂停降耗（R5）：App 切后台后无动态未读网络请求（前台恢复自动续）；播放暂停约 1s 后播放器轮询降频（logcat 中 overlay 轮询 tick 明显变稀）；控制层隐藏时全屏/内联 overlay 进度 tick 降至 1s/2s，点出控制层立即恢复精确进度（无可见滞后）
27. 滑动/加载/过渡（R1~R3）：首页高速滑动与进/出详情页 morph 转场流畅度不劣化；骨架屏在转场中无脉冲抖动；冷启动后点击播放不再有同步磁盘 IO 卡顿
28. UI 细节一致性（S 节）：① 首页信息流混排普通/玻璃/电影感/直播卡**圆角一致**（Card 级 12dp 基准），直播卡不再带阴影"浮起"；加载骨架与真实卡同圆角（加载完成无跳变）；他人主页投稿网格同口径；② 评论区粉丝勋章配色与 `BadgeLevelPalette` 同源（评论区/个人页不撞色）；③ 动态发布→预约直播：选时间 = App 风格「日期→下一步→时间」两级弹窗（无系统 stock 对话框）；搜索筛选选日期 = App 风格弹窗；④ 浅色主题下进播放器：画质/倍速/音质菜单为浅色 surface 容器（不再是黑底白字），禁用项/分隔线层次正常

---

## 6. 冲突裁定与演进备忘（关键）

1. **竖屏播放器**：经历"实现竖屏全屏 → A1 删除 → 详情页 9:16 竖条替代 → **2026-08-21 第三轮：70% 屏高 fit 居中 + 上滑联动缩到 16:9 + tab 固定**"三轮演变。**最终态 = 无 Story 式全屏播放器 + 详情页竖屏 70% 高区域（视频 FIT 居中，黑边对称）+ 上滑跟手缩小（16:9 下限）+ tab 行固定带分割线 + 竖屏全屏按钮无反应**。复现以 I 节为准。
2. **模糊滚动感知**：`c709c15`/`5c31155` 引入 → `4de7c6a` 部分撤销（UnifiedBlur/HomeCategoryPage/VideoCard 已止血）；`WatchLaterScreen` 等 4 文件的滚动感知行仍在（可选项 H）。
3. **主题默认**：本轮把默认主题改为 Material3、模糊默认 OFF（`BlurIntensity.OFF` 全链路）、顶栏默认始终显示。**只影响新装/未设置用户**；设置页选项全部保留，不做硬编码强制。
4. **Round2 文档中已过时/已执行的内容**：
   - Tier 1（UnifiedBlur/HomeCategoryPage/VideoCard 三件套）= 已被 `4de7c6a` 落地 ✅
   - WS3 1080P/90Hz = 已被 `212b58b` 落地 ✅
   - "HomeHeader `resolveHomeTopChromeMotionPolicy` 恢复 BLUR 恒 isScrolling=false" → **过时**（顶部已强制 PLAIN 实色，模糊分支不生效，无需执行）
   - 竖屏播放器设计共识（第 5 节规格）→ **被 A 系列推翻**，不复现
5. **提交哈希适用性**：20 个关键提交哈希（A~P 轮，含 08-25 归档的 `1867841`/`31c83f8`/`cb0d6ad`）已逐一验证**全部存在于本地历史**，`git fetch`/rebase 保留本地历史时可直接 cherry-pick（按 §2.0 步骤 2a）；重新克隆则哈希失效，按本手册功能点手动改（§2.0 步骤 2b）。⚠️ 此前悬空的 I/K/N/O/P 轮 49 文件已于 08-25 分 3 提交归档，工作区已干净。
6. **弹幕清空修复**（第四轮 J 节）：核心口径 = **暂停/缓冲/漂移恢复一律原地续播（SoftResync，不清屏），只有真正的时间线跳变（seek/切集/关开关/改设置）才硬清屏**。漂移校正只重定引擎时钟，不再 `clear()+setData` 重建数据。
7. **切 tab 播放器状态**（第五轮 K 节）：tab 切换**不得**重置播放器折叠状态（切 tab 保持当前大小）；仅"一键回顶"与"返回 morph"恢复。复现时若上游代码有 tab 相关 reset 逻辑一律移除。
8. **PiP 宽高比与最近任务**（第五轮 K 节）：PiP 宽高比 = 按 `player.videoSize` 方向判定（高>宽 → 9:16，否则 16:9，未知回退 16:9）；**最近任务看不到 PiP 应用属 ROM/权限行为，代码无移除逻辑，勿为它改代码**。
9. **列表页精简**（第六轮 L 节）：核心口径 = ① 稍后再看/我的收藏顶栏底纹 = **标题栏 + 搜索框**（我的收藏另含收藏夹切换 chips 行，仍在底纹内），首卡距底纹下沿 8dp；② 我的收藏搜索框**恒固定**（`resolveCommonListHeaderCollapseModeForScreen` isFavoritePage → ALWAYS_VISIBLE，历史页不受影响）；③ 底纹 = 仅 `unifiedBlur`，**不叠加 surface 底色**（与稍后再看同观感）。复现时若上游带回分类 chips / 顶栏折叠 / 底色叠加一律移除。
10. **主页口径**（第七轮 M 节）：① 其他用户主页头部 = **PiliPlus 布局**（头像左下重叠 20/伸出 60 + stats/私信/关注右下栏 + 名字/等级/VIP 独立行）；② 本人主页**无 硬币/经验/经验条/编辑资料**、**无圆角底纹**（内容平铺）；③ 编辑资料入口整体移除（含签名编辑对话框），签名改由其它途径维护。复现时若上游带回 硬币/经验/编辑资料 或底纹容器一律移除。
11. **UI 性能口径**（Q 节）：① **订阅位置**——DataStore/共享 flow 的 collect 一律在屏幕/列表顶层做一次后以参数下传，禁止放进 LazyList/Grid item 与叶子卡片（builder lambda 内也不能调 @Composable）；② **坐标上报**——`onGloballyPositioned` 只存 `LayoutCoordinates` 引用，`boundsInRoot()/boundsInWindow()` 仅在交互回调内 `takeIf { it.isAttached }` 惰性求值；③ **拖动瞬态状态**——手势逐帧写入的状态不得被巨型函数体组合期读取（jank 追踪走 effect 版 / 浮层进度直读不走 animateFloatAsState / spec 记忆键做量化）；④ **聊天滑窗列表**——必须带本地自增 id key + `scrollToItem` + 收集侧过滤；⑤ **窗口属性/binder 写入**——亮度 0.02 阈值节流、音量仅档位变化才 set、maxVolume 缓存。
12. **性能轮口径**（R 节）：① **零视觉损失红线**——1080P@90、视觉规格、动画时长一律不动，只砍"消费者不可见时的开销"（后台轮询/暂停期 tick/隐藏档轮询/挂载期磁盘 IO/转场期重活），且凡放宽间隔必满足"状态切换即 effect key 重启并首迭代立即同步"；② **弹幕引擎勿动**——DanmakuView 是硬件加速 View（Skia glyph 缓存），VOD 已分段窗口，4.1 纹理化与 4.3 trimBefore 均不需要，实机 gfxinfo 证实瓶颈前不做引擎改造；③ **听视频页独立 player 锁屏放行**——ON_PAUSE 暂停决策必须保留 `isAudioOnlySession` 分支（standalone player 不归 MiniPlayerManager，后台音频判定恒 false），上游重构该决策时不得丢失；④ 字幕 cue 是本地数组非 Media3 文本轨道，字幕恢复/唤醒走 `Player.Listener` 事件，勿用 `onCues`。
13. **UI 细节口径**（S 节）：① **圆角一律走 `AppShapes.container(ContainerLevel.*)` 语义层级**——间距 token（`AppSpacingTokens.*`）不作圆角用、不写裸 `RoundedCornerShape(N.dp)`；信息流卡片统一 Card 级，嵌套内层圆角 = 外层 − 内边距；② **等级/勋章类配色单一来源 `BadgeLevelPalette`**（新增 ramp 先进这里再引用）；③ **弹窗一律 `AppAlertDialog`/`AppModalBottomSheet` 封装**——`android.app.*Dialog` 与裸 material3 `*Dialog` 禁用（日期/时间选择 = M3 `DatePicker`/`TimePicker` composable 放进 AppAlertDialog 容器）；④ **播放器浮层菜单用语义色**——`surfaceContainerHigh`/`onSurface`/`outlineVariant`，禁止 `Color(0xFF…)` 容器底色（`HardcodedColorMigrationGuardTest` 名单文件尤其注意）。上游带回旧模式（token 当圆角/裸系统对话框/0xFF 菜单底色/自写空态占位）一律按本节口径重新应用。

---

## 7. 待办（下一轮）

**立即（下次上游更新前必做）**
- ✅ **已完成（08-25）**：工作区 49 个未提交文件已分 3 提交归档——`1867841`（I+K，17 文件）、`31c83f8`（N 轮，24 文件）、`cb0d6ad`（O+P 轮，8 文件），每个提交前 `compileDebugKotlin` 通过。下次更新直接走 §2.0 步骤 1（评估上游差异），无需再归档。

**功能开发（未开始）**
- **A2 播放页布局照搬 PiliPlus**：需 BiliPai 与 PiliPlus 两边播放页截图（正常/全屏/控制栏显隐）→ 产出逐区块差异清单 → 照搬布局/交互/皮肤（保留引擎）
- **Tier 2**（可选）：升 Haze alpha，内置自适应采样
- **FSR 超分**（需求 2）：需手机型号/SoC 信息
- **实机性能验证（R 轮收尾）**：装机（`:app:installDev`）后跑 §5 清单 25~27 项 + gfxinfo 弹幕开关对照（若弹幕被证实为瓶颈再议 §2.R4 的 4.1）
- **git push**：本地 main 领先 origin **40 个提交**（含 08-28 S 轮 4 个）。08-28 尝试 push 失败：非交互终端里凭证助手弹窗被取消（`fatal: User cancelled dialog` / `could not read Username for 'https://github.com'`）——需在交互终端手动 `git push`（或配好 credential manager / SSH 后重试）

**口径已定（复现时保持，无需再确认）**
- 跟手停住无回弹（不做 PiliPlus 的 56px 工具条档与播放状态回弹）；tab 跟随播放器下沿；弹幕全播放器区域；切 tab 保持播放器当前大小

---

## 8. 复刻口诀

**"A 系列删竖屏播放器 + 竖屏详情页 70% 屏高 fit 居中（1a 上下黑边/1b 左右黑边）+ 上滑跟手缩到 16:9（无回弹，tab 固定带分割线，弹幕全播放器区，全屏无反应，切 tab 保持播放器大小）+ 顶部实色 13dp 禁模糊 + 禁壁纸 + 主页搜索 UI + 性能两件套（稳定模糊 + 1080P@90）+ 评论 Tab 批 + 主题默认（Material3 / 模糊 OFF / 顶栏常显，设置保留）+ 弹幕清空修复（暂停/缓冲/漂移改原地续播不清屏，seek 等时间线跳变仍硬清屏）+ PiP 宽高比随视频方向（横 16:9 / 竖 9:16，最近任务无应用属 ROM 行为勿改）+ 列表页精简（稍后再看删全部/未看完 tab，我的收藏删 7 分类标签，搜索恒固定，底纹=标题栏+搜索框、仅 unifiedBlur 不叠底色，首卡距底纹 8dp）+ 主页口径（他人主页头部照搬 PiliPlus：头像左下 20/60 + stats/私信/关注右下栏 + 名字/等级/VIP 独立行；本人主页删 硬币/经验/经验条/编辑资料 与圆角底纹，内容平铺上移）+ 第八/九/十轮（听视频：**整页恒深色**含弹层与系统栏图标（O1）、前景恒白、禁横滑切 tab 切换条常显、歌词=官方→第三方→原生字幕→AI字幕→非中文双语对照（O4）→未找到、列表点击直达播放页、**进度条定位式**：点击/拖动直接定位不吸附（O2 推翻增量式）；非当前歌词行统一 66% alpha/1dp 模糊（O7 推翻距离分级）；首页卡片：统计贴封面默认开+去封面在线人数 pill、**时长默认封面内无底色**（P1）、首页去发布于；他人主页：投稿默认单列、删 tab 下全宽线、统计照抄我的主页 72dp、**按钮行与统计行右边界对齐**（P4 推翻等宽，推导式 250/214/105/87）、右侧列与头像中线对齐、顶栏实色删 haze、删 tab 搜索横条保留功能、去 VIP、动态卡列表隐藏三件套+分隔线详情保留、**根动态图文点击进详情**（P2）；动态详情：评论数加粗删线、切换条 36dp、整页 +2sp；历史记录三块恒固定+实色、稍后再看实色；长按倍速默认 3x+长按震动；UI 性能修复批（Q 节）：弹幕列表本地 id key 化+scrollToItem+收集侧过滤+表情 map 单订阅、全屏层安全区避让+面板返回拦截+seek 卡去 blur、手势死状态删除+jank 追踪 effect 化+档位浮层 spec 5% 量化去动画、动态卡 DataStore 屏幕层单订阅（4 调用方）、滚动坐标只存引用交互时惰性求值、推荐页 O(n) 派生全 remember 化（builder 外）、关注搜索 150ms 去抖+Default 线程拼音缓存过滤、Live 手势尺寸回调内实时读+亮度 0.02 节流+音量档位判变），构建签名交付照流程，基线失败不修。"**

**"R 性能轮：滑动/加载/过渡/后台四路零视觉损失优化——滑动稳帧（磁盘IO消缓存512+镜像预热、故事卡坐标引用化、轮播绘制期变换、pager ±1页 idle 预挂载转场全量、jiggle 改 DrawModifierNode 自驱）+ 加载提速（预加载复用请求 spec 同 key 命中内存缓存、删人工 delay、WBI 空闲预刷新、Coil 内存缓存 18%）+ 过渡稳帧（morph 落定前不启动重活、壁纸去重、fallback snap 等待、clip shape 2px 量化 LruCache、骨架脉冲转场归零）+ 后台降耗（未读轮询后台门控、状态栏采样 200ms、Bangumi/字幕暂停退出 tick 字幕走 Listener 事件唤醒、seek 会话按需同步、overlay 隐藏档放宽 1s/2s——放宽必配'切换即重启首迭代立即同步'）+ 弹幕引擎不动（HW 加速 View 非 CPU 软绘、VOD 已分段窗口）+ 听视频锁屏误暂停修复（standalone player 传 audioOnlySession=true，ON_PAUSE 决策 isAudioOnlySession 分支勿丢）。"**

**"S UI 细节批：信息流卡片圆角统一 AppShapes Card 级（普通 8/电影 16/玻璃与故事 20/直播 14 → 全收敛 12dp 基准随皮肤缩放，间距 token 不再当圆角用，直播卡删孤立阴影，骨架卡与空间页同步，LiveRoomCard 在硬编码色监控名单只删不增）+ 等级徽章配色单一来源 BadgeLevelPalette（LV 兜底 ramp + 粉丝勋章 ramp 数值原样搬迁，直播间 UL 走 LiveStatusPalette 勿重复迁）+ 弹窗收口（动态预约平台 DatePicker/TimePickerDialog → AppAlertDialog+M3 DatePicker/TimePicker 日期→时间两级串联 is24Hour，搜索筛选 M3 DatePickerDialog → AppAlertDialog 容器）+ 播放器画质/倍速/音质菜单主题化（0xFF2B2B2B→surfaceContainerHigh、白字→onSurface、禁用 0.38 alpha、分隔线 outlineVariant；黑 scrim/彩底白字/primary 选中不动）。结构测试是源码字符串断言：改圆角来源行勿动 cardShellShape/coverShape 断言行。"**
