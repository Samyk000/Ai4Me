# Third-Party Notices and Open Source Attributions

Ai4Me is built using open-source libraries, architectures, and open model weights in accordance with **Document 2 §11**. This document records licenses, attributions, and checksum requirements.

---

## 1. Core Runtime Runtimes & Frameworks

### llama.cpp & GGUF
- **Project:** [llama.cpp](https://github.com/ggml-org/llama.cpp)
- **License:** MIT License
- **Copyright:** (c) 2023-2024 Georgi Gerganov and contributors
- **Purpose:** On-device local GGUF model parsing, quantization support, and inference runtime.

### sherpa-onnx / Whisper
- **Project:** [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx) & [whisper.cpp](https://github.com/ggerganov/whisper.cpp)
- **License:** Apache License 2.0 / MIT License
- **Purpose:** Offline local streaming Speech-To-Text (ASR) and keyword spotting (KWS).

### Android Jetpack & Material 3
- **Project:** Android Open Source Project (AOSP) & Jetpack Compose
- **License:** Apache License 2.0
- **Purpose:** Modern native UI toolkit, design system tokens, Material 3 navigation, and lifecycle management.

### Kotlin & Kotlin Coroutines
- **Project:** [JetBrains Kotlin](https://github.com/JetBrains/kotlin)
- **License:** Apache License 2.0
- **Copyright:** (c) 2010-2024 JetBrains s.r.o.
- **Purpose:** Primary language runtime, asynchronous flows, and structured concurrency.

### OkHttp
- **Project:** [Square OkHttp](https://github.com/square/okhttp)
- **License:** Apache License 2.0
- **Copyright:** (c) Square, Inc.
- **Purpose:** Resumable HTTP Range-header model downloader (`bytes=<offset>-`) with SHA-256 pre-flight validation.

---

## 2. Supported Curated Model Weights

Model weights are downloaded directly by the user from public repositories on Hugging Face Hub without requiring proprietary logins:

| Model | Publisher / Repository | License |
| :--- | :--- | :--- |
| **SmolLM2 1.7B Instruct** | Hugging Face (`HuggingFaceTB/SmolLM2-1.7B-Instruct-GGUF`) | Apache 2.0 |
| **Qwen 2.5 3B Instruct** | Alibaba Cloud Qwen (`Qwen/Qwen2.5-3B-Instruct-GGUF`) | Apache 2.0 |
| **Llama 3.2 3B Instruct** | Meta Platforms (`meta-llama/Llama-3.2-3B-Instruct`) | Llama 3.2 Community License |
| **Qwen 2.5 7B Instruct** | Alibaba Cloud Qwen (`Qwen/Qwen2.5-7B-Instruct-GGUF`) | Apache 2.0 |

---

## 3. Privacy & Telemetry Certification
Ai4Me bundles **zero analytics SDKs, zero crash-reporting collectors, and zero user-tracking identifiers**. All data stays on the user's physical hardware.
