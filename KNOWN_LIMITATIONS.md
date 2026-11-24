# Ai4Me: Known Limitations & System Constraints

This document provides transparent, verified disclosures regarding hardware, operating system boundaries, and runtime performance of Ai4Me, in strict compliance with **Document 1 §0, §8, and §10 (Definition of Done)**.

---

## 1. Hardware Architecture & Memory Constraints
Ai4Me performs 100% offline local inference on the host device's physical CPU and GPU/NPU.

| Device RAM Profile | Target Models | Quantization | Usable Context Budget |
| :--- | :--- | :--- | :--- |
| **Low RAM (< 6 GB)** | SmolLM2 1.7B, Qwen 2.5 0.5B | Q4_K_M / Q4_0 | 2,048 tokens |
| **Standard RAM (6–8 GB)** | Qwen 2.5 3B, Llama 3.2 3B | Q4_K_M | 4,096 tokens |
| **High RAM (12+ GB)** | Qwen 2.5 7B | Q4_K_M / Q5_K_M | 4,096 – 8,192 tokens |

### Thermal Throttling Effects
Extended token generation and sustained voice synthesis will generate physical device heat. Modern mobile chipsets (Snapdragon, MediaTek, Tensor) automatically throttle CPU clock frequencies when thermal thresholds are exceeded. Under thermal throttling, generation speed will degrade gracefully by 20% to 40% until core temperatures normalize.

---

## 2. Android Operating System Boundaries

### Android 14+ Background Microphone Lifecycle
Under Android 14 (API 34) and Android 15 (API 35), background audio capture is subject to strict OS-level restrictions:
- The wake-word listening service runs as a `FOREGROUND_SERVICE` with explicit type `FOREGROUND_SERVICE_TYPE_MICROPHONE`.
- Android enforces a persistent notification in the notification shade and a visible green microphone privacy indicator in the status bar while the service is listening.
- A quick **[Stop Listening]** action is directly accessible in the notification to allow immediate revocation of mic access.
- Under extreme low-memory pressure, Android may terminate background foreground services.

### Accessibility Service Scope & Secure Screens
- Ai4Me uses Android's `AccessibilityService` for UI inspection, screen hierarchy traversal, and assistive coordinate gestures.
- **FLAG_SECURE Restriction:** Accessibility cannot read or interact with secure windows, including banking apps, payment gateways, password managers, or private incognito browser tabs.
- The agent will never attempt to bypass OS-level permission dialogs or system security prompts.

### Notification Access
- `NotificationListenerService` can only capture notifications explicitly broadcast by installed applications to the Android status bar. Apps configured to hide sensitive lock-screen notifications will be respected.

---

## 3. Tool Verification & Delivery Guarantees

### Message Delivery Verification
- When sending SMS messages via system intent (`Intent.ACTION_SENDTO`), the host Android OS hands off the message to the default messaging application.
- Because standard Android intents do not return SMS carrier delivery receipts to caller apps, Ai4Me **strictly reports: `"sent action attempted; delivery unverified"`**. It will **never report a false `"delivered"` status**.

### Contact Ambiguity Resolution
- If an instruction specifies a common or duplicate name (e.g., *"Tell Rahul I'll call tomorrow"*), Ai4Me requires explicit user disambiguation (`status = "needs_user_input"`) if more than one matching contact exists.

---

## 4. Offline Boundary Definition
- **Model Provisioning:** Searching and downloading GGUF model files from Hugging Face requires an active internet connection during download.
- **Core Runtime:** Once model weights and voice assets are provisioned, all reasoning, chat, speech recognition (STT), speech synthesis (TTS), local memory queries, and automation operate **100% offline in Airplane Mode with zero telemetry or cloud connectivity**.
