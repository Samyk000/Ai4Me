# Ai4Me --- Engineering Playbook & Reference Implementations

**Document 2 of 3 \| How to build it, what to reuse, and what not to
reinvent**

## 0. Instructions to the coding agent

This is the implementation playbook. Read it together with Document 1.
Before choosing a library or writing a subsystem, inspect the reference
repositories and their current documentation. Pin the exact
version/commit used and record why it was selected.

Do not copy an entire app blindly. Reuse proven patterns, interfaces and
algorithms while keeping Ai4Me's native Android architecture.

## 1. Reference projects and lessons

### 1.1 PocketPal AI --- model library and on-device inference reference

Repository: https://github.com/a-ghorbani/pocketpal-ai

Study: - Model catalogue and Hugging Face browsing. - Model download and
load/unload flows. - GGUF model handling. - Hardware benchmarking. -
Persona/system-prompt configuration. - Chat editing/regeneration. - Tool
registry and agent runner. - Separation between UI, native bridges and
inference engines.

Important architectural observation: PocketPal currently uses React
Native, TypeScript, `llama.rn`/llama.cpp, `whisper.rn`/whisper.cpp,
ONNX-based TTS, MobX and WatermelonDB. Ai4Me is Kotlin + Jetpack
Compose, so do not copy its framework. Reproduce the useful product
patterns with native Kotlin equivalents.

PocketPal's published flow is: 1. Browse a curated model list or search
Hugging Face. 2. Select a GGUF quantization suitable for device
memory/storage. 3. Download or bookmark. 4. Load the model. 5. Chat
locally. 6. Benchmark tokens/second and memory.

Its agent pattern uses a registry of tool schemas. The runner detects a
model tool call, executes the matching engine, and feeds the result back
to the model for follow-up reasoning. This is the pattern Ai4Me should
implement.

### 1.2 llama.cpp --- local LLM runtime

Repository: https://github.com/ggml-org/llama.cpp

Use as the first inference runtime for GGUF models. Read the Android
build documentation, examples and current API before integration. Avoid
pinning an old sample API from a blog post.

Implement behind `LLMProvider`; never expose llama.cpp types throughout
the app.

Required adapter capabilities: - Model load/unload. - Context creation
and release. - Prompt/token input using the model's chat template. -
Token streaming. - Cancellation. - Stop sequences. - Sampling
settings. - Context length. - Performance and memory metrics. - Runtime
errors with actionable messages.

Do not assume every GGUF file has the same chat template. Read model
metadata and support known templates. If a model's template cannot be
identified, warn the user and allow a deliberate override instead of
silently formatting prompts incorrectly.

### 1.3 sherpa-onnx --- speech and keyword spotting

Repository: https://github.com/k2-fsa/sherpa-onnx

Evaluate its Android-supported APIs and available model recipes for: -
Streaming ASR. - VAD. - Keyword spotting. - Offline TTS where supported.

Do not assume every model hosted by a community is licensed for
redistribution. Track model source, license and checksum.

### 1.4 Hugging Face Hub --- public model discovery

Official documentation: - https://huggingface.co/docs/hub -
https://huggingface.co/docs/huggingface_hub

Use public Hub APIs for discovery and metadata. Public
repositories/files should be browsable and downloadable without login.
Gated or private repositories may require an optional user-provided
token and must never be presented as public.

Implement a `ModelCatalogRepository` abstraction: -
`getFeaturedModels()` - `searchModels(query, filters, page, pageSize)` -
`getRepositoryFiles(repoId, revision)` -
`getModelFileMetadata(repoId, filename, revision)` -
`bookmarkModel(modelRef)` - `getLocalModels()`

Use documented Hub endpoints and current response schemas. Do not scrape
HTML pages when an API exists.

Model search should filter for GGUF or other explicitly supported
formats. A Hugging Face repository is not automatically an installable
model. Inspect repository files and identify actual model weight files,
split shards, tokenizer/config metadata and license.

### 1.5 UI/UX and coding-agent references

-   Agency Agents: https://github.com/msitarzewski/agency-agents
-   Emil Kowalski Skills: https://github.com/emilkowalski/skills
-   Awesome AI Tools for UI:
    https://github.com/maxbogo/awesome-ai-tools-for-ui
-   UI UX Pro Max:
    https://github.com/nextlevelbuilder/ui-ux-pro-max-skill

These are development-time tools and review standards. They are not
dependencies shipped inside the Android app.

## 2. Recommended implementation stack

### Android app

-   Kotlin.
-   Jetpack Compose.
-   Material 3.
-   AndroidX Lifecycle/ViewModel.
-   Navigation Compose.
-   Kotlin Coroutines and Flow.
-   Hilt.
-   Room.
-   DataStore.
-   WorkManager.
-   OkHttp for Hub API and controlled file downloads.
-   Kotlin Serialization or Moshi for API/JSON models.
-   AndroidX Security Crypto only after checking current
    maintenance/status and threat model; prefer platform Keystore-backed
    encryption for sensitive keys.

### Native inference and speech

-   llama.cpp through a narrow JNI adapter for GGUF.
-   sherpa-onnx Android bindings for speech/keyword/VAD features where
    supported.
-   ONNX Runtime only where a selected speech model requires it.
-   AudioRecord/AudioTrack for audio capture/playback where required.
-   Android AudioManager/AudioFocus APIs for routing and focus.

### Quality and tests

-   JUnit.
-   MockK or another Kotlin-compatible mocking framework.
-   Turbine for Flow testing if needed.
-   Compose UI tests.
-   Android instrumentation tests.
-   Macrobenchmark/Baseline Profile where useful.
-   Strict lint and static analysis.
-   Gradle dependency verification and version catalog.

Do not add all libraries automatically. Verify current versions, Android
minSdk compatibility, license and maintenance status first. Keep
dependencies minimal.

## 3. Model library: exact product behavior

This is a major product feature, not a simple file picker.

### 3.1 Model screens

Build: 1. **Featured:** Curated starter models, recommended for detected
hardware. 2. **Browse:** Search/filter supported models from Hugging
Face. 3. **Downloads:** Active, queued, paused, failed and completed
downloads. 4. **Installed:** Local model files with metadata and
actions. 5. **Model details:** Description, publisher, license, format,
file size, quantization, context metadata, source repository and
warnings. 6. **Benchmark:** Local tokens/second, prompt processing
speed, memory use and thermal status. 7. **Runtime settings:** Context,
threads, GPU layers/acceleration if supported, sampling and template
override.

### 3.2 No-login public downloads

The user must be able to: - Search public Hugging Face model
repositories without an account. - Open a repository's supported GGUF
files. - Select a quantization. - See exact file size before
downloading. - Download public files without a token. - Pause/resume
downloads. - Retry failures. - Delete downloads. - Load a downloaded
model. - Import a local model using Android Storage Access Framework.

Never show a login wall for public files.

For gated/private repositories: - Explain why authentication is
required. - Offer an optional Hugging Face access token. - Never require
an account for ordinary public model use. - Store tokens using Android
Keystore-backed secure storage. - Never log tokens or include them in
crash reports.

### 3.3 Featured model catalogue

Ship a small, editable JSON catalogue of tested/recommended models. Do
not hardcode the entire catalogue in UI code.

Each catalogue entry should include: - Stable ID. - Display name. -
Publisher. - Hugging Face repository ID. - Exact file name or selection
rule. - Model family. - Parameter count. - Quantization. - Format. -
File size (when verified). - License URL/name. - Supported chat
template. - Context recommendation. - Estimated RAM range with
"estimate" label. - Minimum recommended free storage. - Supported
runtime. - Last verified timestamp. - Compatibility notes. - Source URL.

Only feature a model after its file, license, format and runtime
compatibility have been checked. Mark unverified entries clearly.

### 3.4 Hugging Face discovery flow

1.  User enters a search term or opens a category.
2.  App queries the documented Hugging Face Hub API.
3.  Show repositories that match supported formats.
4.  User opens a repository.
5.  App retrieves repository metadata and file list.
6.  App identifies supported model files, including split GGUF shards if
    implemented.
7.  User selects a file/quantization.
8.  App checks file size, free storage, license and compatibility.
9.  User starts download.
10. Download manager saves to app-managed model storage.
11. Verify file size and SHA-256 when a trusted checksum is available;
    otherwise calculate local SHA-256 for integrity tracking, without
    claiming it proves publisher authenticity.
12. Atomically rename `.part` to final file only after successful
    verification.
13. Parse model metadata.
14. Add it to Installed Models.
15. Offer Load Model.

Use the repository's default branch only when appropriate. Pin a
revision/commit for reproducibility when possible. Do not use an
unpinned mutable URL as the only record of a model.

### 3.5 Download manager

Implement a dedicated `ModelDownloadManager`, not download logic inside
a Compose screen.

Use OkHttp with HTTP Range support and WorkManager for durable
scheduling/retry where appropriate.

Download states: - QUEUED - CONNECTING - DOWNLOADING - PAUSED -
VERIFYING - COMPLETED - FAILED - CANCELLED

Requirements: - Resume using `Range: bytes=<existingLength>-` only when
the server supports it. - Validate `206 Partial Content` and
Content-Range before appending. - If the server returns `200 OK` to a
resume request, restart safely rather than corrupting the file. - Track
ETag/Last-Modified and remote length when available. - Write to
`.part`. - Flush/close safely. - Verify expected length and checksum
where available. - Atomic rename on success. - Retry transient network
errors with bounded exponential backoff. - Do not retry permanent
401/403/404 errors endlessly. - Detect insufficient storage before and
during download. - Allow cancellation and deletion. - Preserve state
across app restarts. - Never load a partially downloaded model. - Show
progress, speed, remaining bytes and a clear failure reason.

Use app-private storage by default. If users choose an external location
through SAF, handle persisted URI permissions and seek/read limitations
correctly.

### 3.6 Model compatibility and safety

Before loading: - Check file extension and actual metadata. - Check
architecture/model family support in the current llama.cpp build. -
Check context and quantization metadata. - Check free RAM and storage. -
Check model shards are complete. - Warn about unsupported multimodal or
architecture features. - Validate chat template. - Run a small
smoke-test prompt.

Never claim "compatible" merely because the filename ends in `.gguf`.

## 4. Inference engine and model lifecycle

Create `LLMProvider` and `ModelRuntimeManager`.

Responsibilities: - Select runtime backend. - Load model off the main
thread. - Create/release inference context. - Stream tokens. - Cancel
generation immediately where supported. - Track model state. - Report
load errors. - Release memory when switching models. - Prevent two large
models loading accidentally.

States:
`NOT_INSTALLED → DOWNLOADED → VALIDATING → READY → LOADING → LOADED → GENERATING → UNLOADING → ERROR`

Do not confuse "downloaded" with "loaded".

### Runtime configuration

Expose safe settings: - Context length. - CPU threads. - GPU
offload/layers if supported. - Batch/micro-batch settings only if the
runtime supports them. - Temperature/top-p/top-k/repetition penalty. -
Max output tokens. - Stop sequences.

Use conservative defaults and benchmark before recommending changes.

## 5. Context manager implementation

Create dedicated classes: - `ConversationRepository` -
`ContextBuilder` - `ContextBudgetManager` - `ConversationSummarizer` -
`MemoryRepository` - `MemoryRetriever` - `TaskContextRepository` -
`PromptTemplateResolver`

### 5.1 Database model

Suggested entities:

`Conversation` - id, title, createdAt, updatedAt, activeModelId,
summary, archivedAt

`Message` - id, conversationId, role, content, createdAt,
tokenCountEstimate, status, parentMessageId

`MemoryItem` - id, category, content, sourceConversationId,
sourceMessageId, createdAt, updatedAt, confidence, sensitivity,
expiresAt, userApproved

`Task` - id, goal, status, createdAt, updatedAt, currentStepIndex,
maxSteps, summary

`TaskStep` - id, taskId, order, toolName, argumentsJson, status,
resultJson, verificationJson, startedAt, completedAt

`ModelRecord` - id, repoId, fileName, localPath/uri, sizeBytes, sha256,
license, format, quantization, contextSize, state

`DownloadRecord` - id, modelRef, url, tempPath, destinationPath,
bytesDownloaded, totalBytes, etag, state, lastError

`ToolAudit` - id, taskId, toolName, riskLevel, decision, result,
timestamp

### 5.2 Context building algorithm

For every model request: 1. Load model's actual context limit. 2.
Resolve its chat template. 3. Tokenize system policy and tool schemas.
4. Reserve output tokens. 5. Add current task state. 6. Retrieve
relevant memory within a small budget. 7. Add recent conversation turns
from newest backwards until budget is reached. 8. If older turns are
omitted, use a stored compact summary. 9. Add current user request last.
10. Validate prompt size before inference. 11. Record budget usage for
diagnostics.

Never silently truncate the current request or tool result. If content
is too large, summarize or ask the user to narrow it.

### 5.3 Summary rules

Session summary should capture: - User's current goal. - Important
decisions and constraints. - Named entities and resolved identities. -
Dates/times/amounts that matter. - Questions still unanswered. -
Promises and pending actions. - Completed and failed tool actions. -
User corrections.

Do not treat model-generated summaries as verified facts. Mark them as
summaries with source links.

### 5.4 Memory extraction

After a conversation/task, optionally propose durable memory candidates.
Apply sensitivity and retention rules. Store only stable, useful,
non-sensitive facts automatically when policy allows. Give users a
memory screen to inspect/edit/delete.

### 5.5 Avoid premature vector database complexity

Start with indexed SQLite search, entity matching and recency. Add local
embeddings only after tests show retrieval quality is insufficient. If
embeddings are added, they must run locally and be optional.

## 6. Agent execution loop

Use a deterministic orchestrator around the LLM. The LLM proposes; code
validates and executes.

Pseudo-flow:

``` text
receive user request
  -> classify intent
  -> retrieve relevant memory
  -> build bounded context
  -> ask LLM for response or structured tool call
  -> validate tool name and JSON schema
  -> check permissions and risk policy
  -> execute one tool
  -> collect actual result
  -> append tool observation
  -> repeat within configured limits
  -> verify task outcome
  -> produce final response
```

Tool contract: - name - description - JSON schema - permission
requirements - risk level - timeout - idempotency behavior - execution
adapter - verification adapter

Tool result contract: - status - success boolean - data - error
code/message - verification evidence - timestamp

Never execute arbitrary code or unregistered tools.

## 7. Android automation implementation

Use: - AccessibilityService for permitted UI inspection and gestures. -
NotificationListenerService for notification content exposed by
Android/apps. - PackageManager for installed app discovery. - Intents
for supported app/system actions. - Contacts Provider only with explicit
permission. - AlarmManager/WorkManager for appropriate reminders and
scheduled work.

Every action should have: - Preconditions. - Timeout. - Cancellation. -
Observation after action. - Verification. - Safe retry policy.

Do not blindly tap coordinates if an accessibility node/action is
available. Coordinate gestures are a fallback and must be device/layout
aware.

## 8. Voice implementation

Use a provider abstraction for: - Wake word. - VAD. - STT. - TTS. -
Audio capture/playback.

Test each component independently before wiring the full conversation
loop.

Wake word: - Must run locally. - Must not require the LLM. - Must expose
false-positive and false-negative diagnostics. - Must respect microphone
foreground-service restrictions. - Must provide visible listening status
and STOP control.

STT: - Partial/final transcript. - Language/model metadata. -
Cancellation. - No network.

TTS: - Voice selection. - Chunked playback. - Barge-in. - Audio focus. -
No network.

## 9. Hardware detection and benchmarking

Build a repeatable benchmark: - Model load time. - Prompt processing
speed. - Generation tokens/second. - Peak and steady-state memory. -
Thermal status. - Battery drain over a defined interval. - STT real-time
factor/latency. - TTS generation latency.

Do not infer NPU support from chipset branding. Detect supported runtime
backends and perform a small test. CPU fallback must always exist where
model architecture permits.

## 10. Build/test gates

For every milestone: - Unit tests for core logic. - Schema tests for
tools. - Context-budget tests. - Download resume/corruption tests. -
Model metadata tests. - Android instrumentation tests. - Offline
airplane-mode tests. - Permission-denial tests. - Lifecycle and
interruption tests. - Performance measurements on at least two device
configurations.

No milestone is "done" without evidence.

## 11. Licensing and supply-chain rules

For each model/library: - Record repository URL. - Pin version/commit. -
Record license. - Check commercial use and redistribution. - Record
model weight license separately from runtime code license. - Record
checksums and provenance. - Keep `THIRD_PARTY_NOTICES.md` updated.

Do not bundle a model unless its license permits redistribution. Prefer
user-initiated downloads from the original publisher.

## 12. Reference sources

PocketPal AI: https://github.com/a-ghorbani/pocketpal-ai

llama.cpp: https://github.com/ggml-org/llama.cpp

sherpa-onnx: https://github.com/k2-fsa/sherpa-onnx

Hugging Face Hub: https://huggingface.co/docs/hub

Hugging Face model API: https://huggingface.co/docs/hub/api

Android developer documentation: https://developer.android.com/

Room: https://developer.android.com/training/data-storage/room

WorkManager:
https://developer.android.com/topic/libraries/architecture/workmanager

Accessibility:
https://developer.android.com/guide/topics/ui/accessibility/service

Notification listener:
https://developer.android.com/reference/android/service/notification/NotificationListenerService
