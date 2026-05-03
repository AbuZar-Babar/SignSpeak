# Graph Report - C:\Users\AbuZar\Desktop\Fyp\signspeak\SignSpeak  (2026-05-03)

## Corpus Check
- 98 files · ~61,023 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 552 nodes · 599 edges · 79 communities detected
- Extraction: 88% EXTRACTED · 12% INFERRED · 0% AMBIGUOUS · INFERRED: 72 edges (avg confidence: 0.78)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- [[_COMMUNITY_Community 0|Community 0]]
- [[_COMMUNITY_Community 1|Community 1]]
- [[_COMMUNITY_Community 2|Community 2]]
- [[_COMMUNITY_Community 3|Community 3]]
- [[_COMMUNITY_Community 4|Community 4]]
- [[_COMMUNITY_Community 5|Community 5]]
- [[_COMMUNITY_Community 6|Community 6]]
- [[_COMMUNITY_Community 7|Community 7]]
- [[_COMMUNITY_Community 8|Community 8]]
- [[_COMMUNITY_Community 9|Community 9]]
- [[_COMMUNITY_Community 10|Community 10]]
- [[_COMMUNITY_Community 11|Community 11]]
- [[_COMMUNITY_Community 12|Community 12]]
- [[_COMMUNITY_Community 13|Community 13]]
- [[_COMMUNITY_Community 14|Community 14]]
- [[_COMMUNITY_Community 15|Community 15]]
- [[_COMMUNITY_Community 16|Community 16]]
- [[_COMMUNITY_Community 17|Community 17]]
- [[_COMMUNITY_Community 18|Community 18]]
- [[_COMMUNITY_Community 19|Community 19]]
- [[_COMMUNITY_Community 20|Community 20]]
- [[_COMMUNITY_Community 21|Community 21]]
- [[_COMMUNITY_Community 22|Community 22]]
- [[_COMMUNITY_Community 23|Community 23]]
- [[_COMMUNITY_Community 24|Community 24]]
- [[_COMMUNITY_Community 25|Community 25]]
- [[_COMMUNITY_Community 26|Community 26]]
- [[_COMMUNITY_Community 27|Community 27]]
- [[_COMMUNITY_Community 28|Community 28]]
- [[_COMMUNITY_Community 29|Community 29]]
- [[_COMMUNITY_Community 30|Community 30]]
- [[_COMMUNITY_Community 31|Community 31]]
- [[_COMMUNITY_Community 32|Community 32]]
- [[_COMMUNITY_Community 33|Community 33]]
- [[_COMMUNITY_Community 34|Community 34]]
- [[_COMMUNITY_Community 35|Community 35]]
- [[_COMMUNITY_Community 36|Community 36]]
- [[_COMMUNITY_Community 37|Community 37]]
- [[_COMMUNITY_Community 38|Community 38]]
- [[_COMMUNITY_Community 39|Community 39]]
- [[_COMMUNITY_Community 40|Community 40]]
- [[_COMMUNITY_Community 41|Community 41]]
- [[_COMMUNITY_Community 42|Community 42]]
- [[_COMMUNITY_Community 43|Community 43]]
- [[_COMMUNITY_Community 44|Community 44]]
- [[_COMMUNITY_Community 45|Community 45]]
- [[_COMMUNITY_Community 46|Community 46]]
- [[_COMMUNITY_Community 47|Community 47]]
- [[_COMMUNITY_Community 48|Community 48]]
- [[_COMMUNITY_Community 49|Community 49]]
- [[_COMMUNITY_Community 50|Community 50]]
- [[_COMMUNITY_Community 51|Community 51]]
- [[_COMMUNITY_Community 52|Community 52]]
- [[_COMMUNITY_Community 53|Community 53]]
- [[_COMMUNITY_Community 54|Community 54]]
- [[_COMMUNITY_Community 55|Community 55]]
- [[_COMMUNITY_Community 56|Community 56]]
- [[_COMMUNITY_Community 57|Community 57]]
- [[_COMMUNITY_Community 58|Community 58]]
- [[_COMMUNITY_Community 59|Community 59]]
- [[_COMMUNITY_Community 60|Community 60]]
- [[_COMMUNITY_Community 61|Community 61]]
- [[_COMMUNITY_Community 62|Community 62]]
- [[_COMMUNITY_Community 63|Community 63]]
- [[_COMMUNITY_Community 64|Community 64]]
- [[_COMMUNITY_Community 65|Community 65]]
- [[_COMMUNITY_Community 66|Community 66]]
- [[_COMMUNITY_Community 67|Community 67]]
- [[_COMMUNITY_Community 68|Community 68]]
- [[_COMMUNITY_Community 69|Community 69]]
- [[_COMMUNITY_Community 70|Community 70]]
- [[_COMMUNITY_Community 71|Community 71]]
- [[_COMMUNITY_Community 72|Community 72]]
- [[_COMMUNITY_Community 73|Community 73]]
- [[_COMMUNITY_Community 74|Community 74]]
- [[_COMMUNITY_Community 75|Community 75]]
- [[_COMMUNITY_Community 76|Community 76]]
- [[_COMMUNITY_Community 77|Community 77]]
- [[_COMMUNITY_Community 78|Community 78]]

## God Nodes (most connected - your core abstractions)
1. `DataCollectorGUI` - 29 edges
2. `HandExtractor` - 22 edges
3. `SignLanguageAugmenter` - 17 edges
4. `DataQualityAnalyzer` - 15 edges
5. `DictionaryViewModel` - 14 edges
6. `AuthViewModel` - 11 edges
7. `run_full_analysis()` - 11 edges
8. `AccountViewModel` - 10 edges
9. `TranslateViewModel` - 10 edges
10. `BackendLiveSignEngine` - 9 edges

## Surprising Connections (you probably didn't know these)
- `run_full_analysis()` --calls--> `SignLanguageAugmenter`  [INFERRED]
  C:\Users\AbuZar\Desktop\Fyp\signspeak\SignSpeak\ml-pipeline\src\analysis\__main__.py → C:\Users\AbuZar\Desktop\Fyp\signspeak\SignSpeak\ml-pipeline\src\features\feature_engineering.py
- `run_full_analysis()` --calls--> `create_augmented_dataset()`  [INFERRED]
  C:\Users\AbuZar\Desktop\Fyp\signspeak\SignSpeak\ml-pipeline\src\analysis\__main__.py → C:\Users\AbuZar\Desktop\Fyp\signspeak\SignSpeak\ml-pipeline\src\features\feature_engineering.py
- `main()` --calls--> `load_actions()`  [INFERRED]
  C:\Users\AbuZar\Desktop\Fyp\signspeak\SignSpeak\ml-pipeline\src\models\evaluate.py → C:\Users\AbuZar\Desktop\Fyp\signspeak\SignSpeak\ml-pipeline\src\config\config.py
- `main()` --calls--> `load_actions()`  [INFERRED]
  C:\Users\AbuZar\Desktop\Fyp\signspeak\SignSpeak\ml-pipeline\src\pipelines\training_pipeline.py → C:\Users\AbuZar\Desktop\Fyp\signspeak\SignSpeak\ml-pipeline\src\config\config.py
- `main()` --calls--> `load_data()`  [INFERRED]
  C:\Users\AbuZar\Desktop\Fyp\signspeak\SignSpeak\ml-pipeline\src\models\evaluate.py → C:\Users\AbuZar\Desktop\Fyp\signspeak\SignSpeak\ml-pipeline\src\data\ingestion.py

## Communities

### Community 0 - "Community 0"
Cohesion: 0.07
Nodes (15): validate_augmented_samples(), compare_sources(), DataQualityAnalyzer, FeatureAnalyzer, load_data(), Isolated data loading function with smart fingerprinting.     Detects if the da, load_actions(), run_full_analysis() (+7 more)

### Community 1 - "Community 1"
Cohesion: 0.09
Nodes (7): DataCollectorGUI, load_actions(), main(), draw_landmarks(), extract_keypoints(), mediapipe_detection(), SeedDictionaryDataSource

### Community 2 - "Community 2"
Cohesion: 0.07
Nodes (7): FaceWireframe, HandExtractionOutput, HandExtractor, HandPoint, HandSide, HandSlot, HandWireframe

### Community 3 - "Community 3"
Cohesion: 0.07
Nodes (5): DefaultAuthRepository, DefaultBookmarkRepository, DefaultComplaintRepository, DefaultDictionaryRepository, DefaultHistoryRepository

### Community 4 - "Community 4"
Cohesion: 0.07
Nodes (5): handleSignOut(), initialsFor(), reporterLabel(), ProfileUiState, ProfileViewModel

### Community 5 - "Community 5"
Cohesion: 0.13
Nodes (19): cross_source_validation(), dataset_statistics(), kfold_cross_validation(), learning_curve_analysis(), main(), noise_robustness_test(), _plot_confusion_matrix(), pretrained_model_evaluation() (+11 more)

### Community 6 - "Community 6"
Cohesion: 0.09
Nodes (5): AuthRepository, BookmarkRepository, ComplaintRepository, DictionaryRepository, HistoryRepository

### Community 7 - "Community 7"
Cohesion: 0.22
Nodes (6): create_augmented_dataset(), SignLanguageAugmenter, test_add_noise(), test_augment_method(), test_spatial_scale(), test_time_warp()

### Community 8 - "Community 8"
Cohesion: 0.12
Nodes (2): DictionaryUiState, DictionaryViewModel

### Community 9 - "Community 9"
Cohesion: 0.14
Nodes (1): BannerTone

### Community 10 - "Community 10"
Cohesion: 0.14
Nodes (4): PredictionReportDraft, PredictionReportReason, TranslateUiState, TranslateViewModel

### Community 11 - "Community 11"
Cohesion: 0.15
Nodes (3): AccountAuthMode, AccountUiState, AccountViewModel

### Community 12 - "Community 12"
Cohesion: 0.15
Nodes (3): AuthMode, AuthUiState, AuthViewModel

### Community 13 - "Community 13"
Cohesion: 0.2
Nodes (1): BackendLiveSignEngine

### Community 14 - "Community 14"
Cohesion: 0.2
Nodes (2): LiveInferenceState, LiveSignEngine

### Community 15 - "Community 15"
Cohesion: 0.2
Nodes (7): BookmarkRow, ComplaintRecord, DictionaryEntry, ProfileRecord, SessionState, SessionUser, TranslationHistoryItem

### Community 16 - "Community 16"
Cohesion: 0.22
Nodes (2): JsonSaveResult, JsonWriter

### Community 17 - "Community 17"
Cohesion: 0.22
Nodes (2): AppLocalStore, Keys

### Community 18 - "Community 18"
Cohesion: 0.22
Nodes (1): UiFormatters

### Community 19 - "Community 19"
Cohesion: 0.25
Nodes (1): MainActivity

### Community 20 - "Community 20"
Cohesion: 0.25
Nodes (2): PredictionAccumulator, PredictionDisplayState

### Community 21 - "Community 21"
Cohesion: 0.25
Nodes (1): SignBackendClient

### Community 22 - "Community 22"
Cohesion: 0.25
Nodes (1): TranslationCatalog

### Community 23 - "Community 23"
Cohesion: 0.25
Nodes (2): VideoFrame, VideoFrameExtractor

### Community 24 - "Community 24"
Cohesion: 0.29
Nodes (1): DictionaryScreen()

### Community 25 - "Community 25"
Cohesion: 0.29
Nodes (0): 

### Community 26 - "Community 26"
Cohesion: 0.33
Nodes (2): BackendSettingsRepository, Keys

### Community 27 - "Community 27"
Cohesion: 0.33
Nodes (1): StreamingSignEngine

### Community 28 - "Community 28"
Cohesion: 0.4
Nodes (3): GeminiSentenceFormer, SentenceLanguage, SentenceResult

### Community 29 - "Community 29"
Cohesion: 0.4
Nodes (1): LabelTranslator

### Community 30 - "Community 30"
Cohesion: 0.4
Nodes (1): MainTab

### Community 31 - "Community 31"
Cohesion: 0.4
Nodes (0): 

### Community 32 - "Community 32"
Cohesion: 0.4
Nodes (2): HistoryUiState, HistoryViewModel

### Community 33 - "Community 33"
Cohesion: 0.4
Nodes (4): Auth, Main, Onboarding, RootDestination

### Community 34 - "Community 34"
Cohesion: 0.5
Nodes (1): IllustrationVariant

### Community 35 - "Community 35"
Cohesion: 0.5
Nodes (0): 

### Community 36 - "Community 36"
Cohesion: 0.5
Nodes (2): RootUiState, RootViewModel

### Community 37 - "Community 37"
Cohesion: 0.5
Nodes (0): 

### Community 38 - "Community 38"
Cohesion: 0.83
Nodes (3): convert_model(), convert_to_tflite(), main()

### Community 39 - "Community 39"
Cohesion: 0.67
Nodes (1): ExampleInstrumentedTest

### Community 40 - "Community 40"
Cohesion: 0.67
Nodes (0): 

### Community 41 - "Community 41"
Cohesion: 0.67
Nodes (0): 

### Community 42 - "Community 42"
Cohesion: 0.67
Nodes (0): 

### Community 43 - "Community 43"
Cohesion: 0.67
Nodes (1): ResponsiveLayoutSpec

### Community 44 - "Community 44"
Cohesion: 0.67
Nodes (0): 

### Community 45 - "Community 45"
Cohesion: 0.67
Nodes (1): ExampleUnitTest

### Community 46 - "Community 46"
Cohesion: 1.0
Nodes (1): InferenceMode

### Community 47 - "Community 47"
Cohesion: 1.0
Nodes (1): AppContainer

### Community 48 - "Community 48"
Cohesion: 1.0
Nodes (1): SignSpeakApp

### Community 49 - "Community 49"
Cohesion: 1.0
Nodes (1): AppJson

### Community 50 - "Community 50"
Cohesion: 1.0
Nodes (1): SupabaseService

### Community 51 - "Community 51"
Cohesion: 1.0
Nodes (0): 

### Community 52 - "Community 52"
Cohesion: 1.0
Nodes (0): 

### Community 53 - "Community 53"
Cohesion: 1.0
Nodes (0): 

### Community 54 - "Community 54"
Cohesion: 1.0
Nodes (1): OnboardingPage

### Community 55 - "Community 55"
Cohesion: 1.0
Nodes (0): 

### Community 56 - "Community 56"
Cohesion: 1.0
Nodes (0): 

### Community 57 - "Community 57"
Cohesion: 1.0
Nodes (0): 

### Community 58 - "Community 58"
Cohesion: 1.0
Nodes (0): 

### Community 59 - "Community 59"
Cohesion: 1.0
Nodes (0): 

### Community 60 - "Community 60"
Cohesion: 1.0
Nodes (0): 

### Community 61 - "Community 61"
Cohesion: 1.0
Nodes (0): 

### Community 62 - "Community 62"
Cohesion: 1.0
Nodes (0): 

### Community 63 - "Community 63"
Cohesion: 1.0
Nodes (0): 

### Community 64 - "Community 64"
Cohesion: 1.0
Nodes (0): 

### Community 65 - "Community 65"
Cohesion: 1.0
Nodes (0): 

### Community 66 - "Community 66"
Cohesion: 1.0
Nodes (0): 

### Community 67 - "Community 67"
Cohesion: 1.0
Nodes (0): 

### Community 68 - "Community 68"
Cohesion: 1.0
Nodes (0): 

### Community 69 - "Community 69"
Cohesion: 1.0
Nodes (0): 

### Community 70 - "Community 70"
Cohesion: 1.0
Nodes (0): 

### Community 71 - "Community 71"
Cohesion: 1.0
Nodes (0): 

### Community 72 - "Community 72"
Cohesion: 1.0
Nodes (0): 

### Community 73 - "Community 73"
Cohesion: 1.0
Nodes (0): 

### Community 74 - "Community 74"
Cohesion: 1.0
Nodes (0): 

### Community 75 - "Community 75"
Cohesion: 1.0
Nodes (0): 

### Community 76 - "Community 76"
Cohesion: 1.0
Nodes (0): 

### Community 77 - "Community 77"
Cohesion: 1.0
Nodes (0): 

### Community 78 - "Community 78"
Cohesion: 1.0
Nodes (0): 

## Knowledge Gaps
- **50 isolated node(s):** `Keys`, `SentenceLanguage`, `SentenceResult`, `HandPoint`, `HandSide` (+45 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **Thin community `Community 46`** (2 nodes): `InferenceMode.kt`, `InferenceMode`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 47`** (2 nodes): `AppContainer`, `AppContainer.kt`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 48`** (2 nodes): `SignSpeakApp.kt`, `SignSpeakApp`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 49`** (2 nodes): `AppJson`, `AppJson.kt`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 50`** (2 nodes): `SupabaseService.kt`, `SupabaseService`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 51`** (2 nodes): `AccountScreen()`, `AccountScreen.kt`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 52`** (2 nodes): `AuthScreen()`, `AuthScreen.kt`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 53`** (2 nodes): `appViewModelFactory()`, `AppViewModelFactory.kt`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 54`** (2 nodes): `OnboardingModels.kt`, `OnboardingPage`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 55`** (2 nodes): `OnboardingScreen.kt`, `OnboardingScreen()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 56`** (2 nodes): `Theme.kt`, `KotlinFrontendTheme()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 57`** (2 nodes): `test_ingestion.py`, `test_ingestion_imports()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 58`** (1 nodes): `vite.config.ts`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 59`** (1 nodes): `main.tsx`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 60`** (1 nodes): `types.ts`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 61`** (1 nodes): `vite-env.d.ts`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 62`** (1 nodes): `supabase.ts`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 63`** (1 nodes): `build.gradle.kts`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 64`** (1 nodes): `settings.gradle.kts`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 65`** (1 nodes): `build.gradle.kts`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 66`** (1 nodes): `Color.kt`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 67`** (1 nodes): `Shape.kt`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 68`** (1 nodes): `run_analysis.py`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 69`** (1 nodes): `__init__.py`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 70`** (1 nodes): `run.py`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 71`** (1 nodes): `__init__.py`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 72`** (1 nodes): `__init__.py`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 73`** (1 nodes): `__init__.py`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 74`** (1 nodes): `__init__.py`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 75`** (1 nodes): `__init__.py`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 76`** (1 nodes): `__init__.py`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 77`** (1 nodes): `__init__.py`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 78`** (1 nodes): `conftest.py`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `run_full_analysis()` connect `Community 0` to `Community 7`?**
  _High betweenness centrality (0.037) - this node is a cross-community bridge._
- **Why does `load_data()` connect `Community 0` to `Community 1`, `Community 5`?**
  _High betweenness centrality (0.036) - this node is a cross-community bridge._
- **Are the 5 inferred relationships involving `SignLanguageAugmenter` (e.g. with `run_full_analysis()` and `test_time_warp()`) actually correct?**
  _`SignLanguageAugmenter` has 5 INFERRED edges - model-reasoned connections that need verification._
- **Are the 7 inferred relationships involving `DataQualityAnalyzer` (e.g. with `TestDetectEmptyLandmarks` and `TestValidateCoordinateRanges`) actually correct?**
  _`DataQualityAnalyzer` has 7 INFERRED edges - model-reasoned connections that need verification._
- **What connects `Keys`, `SentenceLanguage`, `SentenceResult` to the rest of the system?**
  _50 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Community 0` be split into smaller, more focused modules?**
  _Cohesion score 0.07 - nodes in this community are weakly interconnected._
- **Should `Community 1` be split into smaller, more focused modules?**
  _Cohesion score 0.09 - nodes in this community are weakly interconnected._