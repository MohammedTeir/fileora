# Fileora — Android AI file assistant — Full Product Specification (v4, Cloud AI only)

## 1. Vision

Build a general-purpose Android AI agent that lets a user control, organize, diagnose, automate, and operate their device through natural language.

The product is **not a VOA app**, **not an FFmpeg app**, and **not merely an AI terminal**.

VOA processing and FFmpeg are examples of what the agent can do.

The real product is:

> **An AI operator for Android that understands a goal, inspects the device, plans the required actions, selects the right tools, executes them safely, analyzes every result, recovers from failures when possible, verifies the final result, and reports what happened.**

It should work in:

- Non-root Android mode
- Rooted Android mode
- A built-in Linux/CLI environment
- Offline mode for all non-AI operations (AI planning and diagnosis require an internet connection)
- Cloud-AI mode using a user-selected provider and model

---

# 2. The Core Idea

The user should normally describe **what they want**, not how to do it.

Instead of:

```bash
find ...
ffmpeg ...
mv ...
mkdir ...
```

the user says:

> "Take all the videos in this folder, convert the audio to MP3, skip anything named Main, and put the results in another folder."

The AI determines:

1. What the user means
2. Which files are relevant
3. Which tools are available
4. Which permissions are needed
5. Which operations are safe
6. What plan should be executed
7. How to verify success
8. What to do if something fails

---

# 3. The Most Important Feature: Observe → Diagnose → Recover

The agent must not stop simply because a command returns an error.

The core execution loop is:

```text
USER GOAL
    ↓
UNDERSTAND
    ↓
INSPECT
    ↓
PLAN
    ↓
PERMISSION / SAFETY CHECK
    ↓
EXECUTE
    ↓
OBSERVE
    ↓
VERIFY
    │
    ├── SUCCESS ───────────────→ REPORT
    │
    └── FAILURE / UNEXPECTED RESULT
                    ↓
                INVESTIGATE
                    ↓
                 DIAGNOSE
                    ↓
              FIND POSSIBLE FIX
                    ↓
          SAFETY / PERMISSION CHECK
                    ↓
                  APPLY FIX
                    ↓
                  RETRY
                    ↓
                 VERIFY AGAIN
                    │
                    ├── SUCCESS → REPORT
                    │
                    └── FAILURE → INVESTIGATE AGAIN
```

The AI is therefore an **adaptive agent**, not a command generator.

---

# 3A. AI Provider and Model Architecture

The first AI provider implementation will use **Google Gemini**. Gemini should be implemented as the first provider, but the architecture must remain provider-independent so the product is not locked to one company or model.

The product requirement is:

> **The user supplies their own API key and chooses which AI model powers the agent.**

The AI layer should sit behind a provider abstraction:

```text
                 AI Agent Core
                       ↓
              Model / Provider API
                       ↓
              ┌────────┴────────┐
              ↓                 ↓
           Gemini        Future Providers
```

## First Provider: Gemini

The first release should prioritize Gemini integration for:

- natural-language understanding
- planning
- structured tool calls
- error diagnosis
- recovery decisions
- summarization
- optional vision/multimodal tasks where supported

The provider implementation must not leak Gemini-specific assumptions into the Agent Core.

## Provider Abstraction

Define a common interface such as:

```text
AIProvider
 ├── listModels()
 ├── validateCredentials()
 ├── generate()
 ├── stream()
 ├── generateWithTools()
 ├── generateStructured()
 ├── estimateUsage()
 └── getCapabilities()
```

Each provider adapter translates this common interface into that provider's API.

## API Key Manager

The app should provide a dedicated API key/settings area. The user can:

```text
Add provider
Edit provider
Test connection
Select model
Set default model
Remove credentials
```

For Gemini, the initial setup can be:

```text
Provider: Gemini
API Key: ********
Model: [user selected Gemini model]
[ Test Connection ]
[ Save ]
```

API keys must:

- be stored using Android Keystore-backed secure storage where possible
- never be written to normal logs
- never be included in AI prompts
- never be exposed to tools unless a tool explicitly needs them
- be redacted from crash reports and audit logs
- be removable by the user

The UI should never display a complete stored key after initial entry.

## Model Selection

Model selection is a first-class product feature.

The user should be able to choose:

```text
Default AI Provider: Gemini
Default Model: [selected Gemini model]

Optional task models:
Planning       → [model]
Simple tasks   → [model]
Error recovery → [model]
Vision         → [model]
Coding         → [model]
```

The first version may expose only one selected Gemini model, but the architecture should already support multiple models.

## Automatic Model Routing

Later, the agent can select a model automatically according to task requirements:

```text
Simple file listing
    → fast / inexpensive model

Complex recovery
    → stronger reasoning model

Image understanding
    → vision-capable model

Large project analysis
    → long-context model
```

The user must be able to disable automatic routing and force one model.

## Model Capability Registry

Every configured model should have capability metadata such as:

```text
tool calling
structured output
vision
long context
streaming
reasoning quality
maximum context
input modalities
output modalities
```

The Agent Core uses capabilities to avoid assigning unsupported work to a model.

## Custom / Compatible Providers

The architecture should eventually support compatible hosted APIs and custom endpoints:

```text
Provider name
Base URL
API key
Model name
API format
```

This allows advanced users to connect compatible providers without changing the agent architecture.

## API Failure Recovery

AI API failures are part of the same recovery architecture as tool failures.

Examples:

```text
Invalid API key
→ validate credentials
→ ask user to update key

Rate limit
→ wait/backoff
→ retry within policy

Model unavailable
→ check model health/configuration
→ use configured fallback if allowed

Context too large
→ reduce/summarize context
→ retry

Tool-call format rejected
→ validate schema
→ repair request
→ retry

No internet connection
→ pause AI-dependent steps
→ continue non-AI work if possible
→ resume when connectivity returns
```

The app should distinguish provider/API failures from device-tool failures.

## Fallback Models

The user may configure a fallback model/provider.

Example:

```text
Primary: Gemini model A
Fallback: Gemini model B

If A is temporarily unavailable:
    verify failure
    → apply retry policy
    → switch to B if permitted
    → continue task
```

Fallbacks must respect the user's privacy preferences.

## Usage and Cost Tracking

Where provider APIs expose usage information, the app should record:

```text
provider
model
request count
input tokens
output tokens
estimated cost when available
latency
errors
```

This information should be visible in an optional AI usage screen.

## Privacy Mode

The user should be able to control what is sent to the AI provider.

Examples:

```text
Send command output        ✓
Send file names            ✓
Send file contents         ✗
Send media to cloud        ✗
Send selected image        ✓
```

The agent should send the minimum information necessary for a task.

## Important Architectural Rule

The Agent Core must never contain code such as:

```text
if provider == Gemini:
    special_agent_logic()
```

Instead:

```text
Agent Core
    ↓
AI Provider Interface
    ↓
Gemini Adapter
```

This keeps Gemini as the first provider while preserving the ability to add other providers later.

---

# 4. What Happens After Investigation?

Investigation is not the end.

After investigating, the agent should classify the situation and choose an action.

Possible outcomes:

```text
SAFE_AUTOFIX
RETRY_WITH_ADAPTATION
INSTALL_MISSING_DEPENDENCY
USE_ALTERNATIVE_TOOL
CHANGE_CONFIGURATION
REQUEST_PERMISSION
ASK_USER
WAIT_FOR_RESOURCE
BLOCK_UNSAFE_OPERATION
ESCALATE_TO_ROOT
FAIL_WITH_DIAGNOSIS
```

Example:

```text
Command:
ffmpeg ...

Result:
Unknown encoder 'libmp3lame'
```

The agent investigates:

```text
1. Check FFmpeg version
2. Check available encoders
3. Check installed packages
4. Determine whether another MP3 encoder exists
5. Determine whether the package/build is incomplete
```

Then it acts.

For example:

```text
Install missing dependency
        ↓
Verify installation
        ↓
Retry FFmpeg
        ↓
Verify generated MP3
```

The user should not have to manually diagnose every failure.

---

# 5. General-Purpose Tool System

FFmpeg is only one tool.

The architecture should support many tools.

## File Tools

```text
file.list
file.search
file.stat
file.copy
file.move
file.rename
file.mkdir
file.delete
file.restore
file.hash
file.compare
file.compress
file.extract
file.watch
file.permissions
```

## Media Tools

```text
media.probe
media.convert
media.extract_audio
media.extract_frames
media.thumbnail
media.resize
media.compress
media.concat
media.split
media.normalize_audio
media.transcode
```

FFmpeg can implement many of these internally.

## System Tools

```text
system.info
system.storage
system.memory
system.cpu
system.battery
system.network
system.processes
system.environment
system.permissions
system.logs
```

## Linux Tools

```text
linux.shell
linux.process
linux.environment
linux.filesystem
linux.service
linux.package
```

## Development Tools

```text
git.clone
git.pull
git.checkout
git.status
git.diff
python.run
node.run
java.run
gradle.build
project.test
project.build
```

## Android Tools

```text
android.apps
android.app_info
android.open_app
android.intent
android.permissions
android.storage
android.settings
android.package_install
android.package_uninstall
```

## Root Tools

```text
root.file_operation
root.process
root.service
root.permissions
root.system_info
root.typed_operation
```

An unrestricted root shell should be treated as an exceptional capability, not the default.

---

# 6. Typed Tools Instead of Blind Shell

The AI should preferably call structured tools.

Example:

```json
{
  "tool": "file.copy",
  "source": "/Download/a.mp3",
  "destination": "/Music/a.mp3"
}
```

Instead of generating:

```bash
cp /Download/a.mp3 /Music/a.mp3
```

This provides:

- Better validation
- Better permissions
- Better logging
- Better error handling
- Better rollback
- Better security
- Better user explanations

A shell can still exist for legitimate Linux tasks, but it should run through a controlled execution layer.

---

# 7. Universal Command Execution Layer

When a shell command is necessary, the execution layer should capture:

```text
command
environment
working directory
exit code
stdout
stderr
duration
CPU usage
memory usage
created files
modified files
process status
```

The AI receives the relevant result and decides what to do next.

Example:

```text
Exit code: 1

stderr:
Permission denied

Created files:
none
```

The agent can then diagnose:

> The command failed because the current environment does not have write permission to the destination.

Then it can:

- choose an accessible destination
- request permission
- use SAF
- use a root backend if explicitly authorized
- ask the user

---

# 8. Verification Is Mandatory

A command returning exit code `0` does not automatically mean the task succeeded.

The agent should verify the actual result.

Example:

```text
Command:
convert image.png output.jpg

Exit code:
0

Verification:
output.jpg exists
size = 0 bytes
```

The agent should classify this as:

```text
FAILED_VERIFICATION
```

and investigate.

For media:

```text
file exists
+
file size reasonable
+
correct MIME type
+
media decoder can open it
+
expected duration/streams exist
```

For file operations:

```text
source exists before operation
destination exists after operation
hashes match when appropriate
```

For package installation:

```text
package installed
+
binary available
+
binary executes
+
version detected
```

---

# 9. Adaptive Error Recovery

The agent should recognize common error classes.

## Dependency Error

```text
ModuleNotFoundError
command not found
library missing
package not installed
```

Possible action:

```text
identify dependency
→ determine package manager
→ install if allowed
→ verify
→ retry
```

## Permission Error

```text
Permission denied
EACCES
read-only filesystem
```

Possible action:

```text
inspect permissions
→ find accessible path
→ request Android permission
→ ask for confirmation
→ use root if explicitly enabled
```

## Wrong Path

```text
No such file or directory
```

Possible action:

```text
search nearby
→ resolve actual path
→ verify
→ retry
```

## Existing File

```text
File already exists
```

Possible action:

```text
compare
→ ask whether overwrite is allowed
→ rename automatically if user preference permits
→ skip if identical
```

## Unsupported Format

```text
Unknown format
Codec unavailable
Decoder unavailable
```

Possible action:

```text
probe file
→ inspect available codecs
→ find alternative conversion method
→ install dependency if possible
→ retry
```

## Network Error

```text
DNS failure
timeout
connection refused
```

Possible action:

```text
check connectivity
→ retry with backoff
→ change mirror if supported
→ continue offline if possible
→ report if external resource is required
```

## Storage Error

```text
No space left on device
```

Possible action:

```text
calculate required space
→ inspect temporary files
→ identify safe cleanup
→ ask user before deleting
→ use another destination
→ resume
```

---

# 10. Recovery Attempts Must Be Controlled

The agent should not retry forever.

Example default:

```text
Maximum automatic recovery attempts: 3
```

But the number can be configurable.

Every attempt is recorded:

```text
Attempt 1
Cause:
missing dependency

Action:
install dependency

Result:
failed

Attempt 2
Cause:
unsupported codec

Action:
use alternative codec

Result:
failed

Attempt 3
Cause:
wrong output configuration

Action:
change conversion parameters

Result:
success
```

The agent should avoid repeating a failed strategy unless new information justifies it.

---

# 11. Failure Memory During a Task

The agent should maintain task state.

Example:

```text
Known facts:

- FFmpeg is installed
- libX is missing
- destination is writable
- source is valid
- strategy A already failed
- strategy B has not been attempted
```

This prevents loops such as:

```text
try A
fail
try A
fail
try A
fail
```

---

# 12. Final Task States

Every task should finish in a defined state.

```text
SUCCESS
SUCCESS_AFTER_RECOVERY
PARTIAL_SUCCESS
WAITING_FOR_USER
WAITING_FOR_PERMISSION
WAITING_FOR_RESOURCE
BLOCKED
UNSAFE_TO_CONTINUE
FAILED_AFTER_INVESTIGATION
CANCELLED
```

Example:

```text
PARTIAL_SUCCESS

153 / 156 files completed.

3 files could not be processed.

Cause:
Unsupported codec.

I investigated 3 recovery strategies but no compatible decoder
is currently available.

Options:
[Install additional codec support]
[Skip files]
[Show details]
```

---

# 13. Safety Architecture

The AI should never directly decide that it is allowed to do anything it wants.

Use:

```text
AI
 ↓
Tool Request
 ↓
Policy Engine
 ↓
Permission Check
 ↓
Risk Classification
 ↓
User Confirmation if required
 ↓
Executor
```

The AI proposes.

The policy engine authorizes.

The executor performs.

---

# 14. Risk Levels

Example:

```text
LEVEL 0 — READ
List files
Inspect metadata
Check device status

LEVEL 1 — LOW IMPACT
Create directory
Copy file
Convert media
Install user-space package

LEVEL 2 — MODERATE
Rename
Move
Change non-critical configuration

LEVEL 3 — DESTRUCTIVE
Delete
Overwrite
Mass modifications

LEVEL 4 — PRIVILEGED
Root operations
System changes
Protected locations

LEVEL 5 — CRITICAL
Operations that could damage the OS, data, or security
```

Policies should be configurable.

---

# 15. Confirmation System

The AI should avoid asking the user about every tiny operation.

Bad UX:

```text
Copy file 1?
Copy file 2?
Copy file 3?
...
```

Instead:

```text
I need to copy 1,284 files.

This is a low-risk operation.

[Allow Once]
[Always Allow for This Task]
[Cancel]
```

For dangerous actions:

```text
I am about to delete 342 files.

This cannot be safely undone.

[Cancel]
[Review Files]
[Confirm Delete]
```

---

# 16. Dry Run

Every complex task should support:

> "Show me what you would do first."

Example:

```text
DRY RUN

Found:
2,481 files

Would move:
184 APK → APK/
322 images → Images/
98 videos → Videos/

Would delete:
27 duplicates

Would compress:
14 archives

No changes made.
```

---

# 17. Undo and Transaction System

Operations should be grouped into transactions.

```text
Task #1842

Operation 1:
copy A → B

Operation 2:
rename C → D

Operation 3:
move E → F
```

If possible, provide:

```text
[Undo Task]
```

For deletions, use a trash/recovery mechanism whenever technically possible.

Not every root/system operation can be safely reversible, so the UI must state that clearly.

---

# 18. General Linux Environment

The application should provide a Linux userspace similar in concept to a Termux-like environment.

Possible components:

```text
bash
coreutils
grep
sed
awk
find
tar
zip
unzip
python
git
ffmpeg
ffprobe
jq
curl
wget
ripgrep
7zip
imagemagick
```

The package list should be expandable.

The goal is not to package every possible program into the APK.

---

# 19. Package Architecture

Use:

```text
Android APK
    ↓
Bootstrap
    ↓
Linux Runtime
    ↓
Package Manager
    ↓
Repository / Cache
    ↓
Installed Packages
```

The AI can request dependencies.

Example:

```text
User:
"Resize these images."

Agent:
ImageMagick is available as an optional package.

Install it?

[Install]
```

Or:

> "Use whatever tools are necessary."

The agent can calculate dependencies and request approval according to policy.

---

# 20. Package Manager Intelligence

The package manager should expose:

```text
search
install
remove
update
upgrade
info
list
verify
repair
rollback where supported
```

The AI should understand package-manager errors.

Example:

```text
Package not found
```

Instead of stopping, it can investigate:

```text
1. Search package database
2. Check package name alternatives
3. Check repository configuration
4. Check architecture
5. Check runtime version
6. Check network
7. Try compatible alternative
```

---

# 21. Non-Root Android Mode

The app should be useful without root.

Use Android's supported mechanisms:

- Storage Access Framework
- MediaStore
- App-specific storage
- User-selected folders
- Appropriate Android permissions

The agent should know exactly what it can access.

Example:

```text
Accessible:

/Download
/Music
/Movies
/VOA_Lessons

Restricted:

/Android/data
/system
```

An optional All Files Access mode may be offered through Android's special app-access settings for users who explicitly need broad shared-storage access. SAF remains the default. The app must check the `MANAGE_EXTERNAL_STORAGE` grant before using direct paths, keep its filesystem sandbox and confirmation/undo protections, and clearly explain the broader scope. Android app-specific paths under `Android/data` and `Android/obb` remain blocked; the agent must never imply that this permission grants root or private app-data access.

The agent should not pretend it can access restricted areas.

---

# 22. Root Mode

Root is an optional backend.

```text
Normal Mode
    ↓
Android APIs / SAF / Linux userspace

Root Mode
    ↓
Permission Layer
    ↓
su
    ↓
Typed Root Operations
```

Root should be disabled by default.

The user explicitly enables it.

The app should detect:

```text
Root available
Root unavailable
Root permission denied
Root manager unavailable
```

---

# 23. Root Safety

Avoid:

```text
AI → arbitrary root shell
```

Prefer:

```text
AI
 ↓
Typed root operation
 ↓
Policy engine
 ↓
Risk analysis
 ↓
Confirmation
 ↓
Root executor
```

For exceptional shell access:

- require explicit opt-in
- show the exact command
- show target paths
- show risk
- log execution
- block known dangerous patterns unless explicitly overridden by a deliberate user action

---

# 24. Device Awareness

The agent should inspect the environment before acting.

Possible information:

```text
Android version
CPU architecture
RAM
free storage
battery
charging state
root status
available Linux runtime
installed packages
available binaries
network state
permissions
accessible storage locations
```

This allows the AI to adapt its plan.

---

# 25. Resource Awareness

Before expensive operations:

```text
Task:
Convert 80 videos.

Estimated temporary storage:
6.2 GB

Available:
2.1 GB
```

The agent should not blindly start.

It can propose:

```text
Options:

1. Process one file at a time
2. Use a different output directory
3. Reduce temporary storage
4. Free storage
5. Cancel
```

Similarly, it can consider:

- battery
- thermal state where available
- CPU load
- memory
- network usage

---

# 26. Background Tasks

Long operations should continue through an appropriate Android background/foreground execution mechanism.

Example:

```text
VOA conversion

Lesson 24 / 52

71 / 156 files

Current:
Lesson_24_Speaking.mp4

Success:
71

Failed:
0

[Pause]
[Cancel]
[Details]
```

If Android stops the process, the task should resume from its checkpoint when possible.

---

# 27. Checkpointing

Large tasks should be resumable.

Example:

```text
Task:
Convert 10,000 images

Completed:
4,218

Checkpoint saved.

App closed.

When resumed:
continue from 4,219
```

Do not start everything again unnecessarily.

---

# 28. File Intelligence

The agent should understand file relationships.

For example:

```text
Lesson_01_Conversation.mp3
Lesson_01_Pronunciation.mp4
Lesson_01_Speaking.mp4
Lesson_01_Main.mp4
```

It can infer that these belong to the same lesson.

This allows higher-level operations:

> "Process every lesson."

rather than requiring individual filenames.

---

# 29. Media Intelligence

The agent should inspect media before choosing a conversion strategy.

Example:

```text
Input:
video.mp4

Detected:
Video codec: H.264
Audio codec: AAC
Duration: 00:08:24
Audio streams: 1
```

Then determine whether audio can be copied or needs transcoding.

This can reduce unnecessary processing.

---

# 30. General Error Intelligence

The error engine should combine:

```text
Exit code
+
stdout
+
stderr
+
tool metadata
+
environment
+
file state
+
permissions
+
previous attempts
+
task goal
```

The AI should not diagnose from stderr alone.

---

# 31. Verification Engine

Create a separate verification subsystem.

Examples:

```text
verify.file_exists
verify.file_size
verify.hash
verify.media
verify.archive
verify.package
verify.process
verify.directory_structure
```

Example:

```text
Expected:
156 MP3 files

Found:
156

Invalid:
0

Missing:
0

Verification:
PASSED
```

---

# 32. Agent Memory

The agent can remember user preferences such as:

```text
"Never delete automatically."

"Put converted audio in /Music/Converted."

"Ask before installing packages."

"Use high quality for MP3."

"Don't use root unless I explicitly request it."
```

Memory must be:

- visible
- editable
- removable
- scoped
- privacy-conscious

---

# 33. Task History

Store completed tasks.

```text
Today

✓ Convert videos
✓ Organize downloads
✓ Install Python
✗ Build project
```

Each task should expose:

```text
Goal
Plan
Tools used
Actions
Errors
Recovery attempts
Files changed
Final result
```

---

# 34. Agent Logs

Provide a human-readable execution log.

Example:

```text
12:31:02  Task started
12:31:03  Scanning source directory
12:31:04  Found 52 lesson folders
12:31:04  Planning conversion
12:31:05  Lesson_01 started
12:31:18  Lesson_01 verified
12:31:19  Lesson_02 started
...
```

Also keep machine-readable structured logs for debugging.

---

# 35. AI Model Architecture

The app should not be locked to one AI provider.

```text
Agent Core
    │
    ├── AI Provider Interface
    │       ├── Gemini (first provider)
    │       └── Future Providers
    │
    └── User-supplied API key + model selection
```

The agent receives:

- User goal
- Available tools
- Tool schemas
- Relevant environment information
- Permissions
- Task state
- Previous tool results

It should receive only the information needed for the task.

All tool output and file content must be treated as **untrusted data**, never as instructions (protection against prompt injection from file names, file contents, web pages, and command output).

---

# 36. Privacy Architecture

Default execution should be local to the device (tools, not AI).

```text
Files → local
Media conversion → local
Linux → local
Package execution → local
Logs → local
```

Only the AI reasoning step uses the cloud. When cloud AI is used, avoid uploading unnecessary files.

For example, the AI may receive:

```text
file names
file sizes
metadata
error messages
directory structure
```

instead of the actual personal media.

---

# 37. Offline Mode

Without Internet, the application should still support:

```text
file management
media conversion
compression
Linux commands
installed packages
task history
verification
manual mode (user-written commands and saved plans)
```

AI planning, diagnosis, and recovery require an internet connection and a reachable provider. Online-only operations should be clearly identified, and AI-dependent steps should pause and resume when connectivity returns.

---

# 38. Tool Plugin System

The architecture should allow new tools to be added.

Example:

```text
Tool Plugin

Name:
PDF Tools

Capabilities:
pdf.merge
pdf.split
pdf.compress
pdf.extract_text
pdf.render
```

Another plugin:

```text
Image Tools

Capabilities:
image.resize
image.convert
image.compress
image.remove_background
```

The AI discovers available tools through schemas.

---

# 39. Tool Schema

Each tool should describe:

```json
{
  "name": "media.extract_audio",
  "description": "Extract audio from a media file",
  "parameters": {
    "input": "path",
    "output": "path",
    "format": "string"
  },
  "risk": "low",
  "requires": ["media_access"]
}
```

This lets the AI understand what a tool can and cannot do.

---

# 40. Capability Discovery

Before planning, the agent can query:

```text
What tools exist?
What packages are installed?
What binaries are available?
What permissions exist?
What storage is accessible?
Is root available?
Is Linux runtime available?
```

This prevents the AI from assuming capabilities that do not exist.

---

# 41. Example: Generic File Organization

User:

> "Clean my Downloads folder."

Agent:

```text
Inspecting Downloads...

Found:
2,481 files

I classified:
Images: 812
Videos: 203
Documents: 631
Archives: 127
APKs: 184
Other: 524
```

Plan:

```text
Create folders
Move files
Detect duplicates
Do not delete anything automatically
```

User confirms.

Agent executes.

If a move fails:

```text
Error:
Permission denied
```

Agent investigates.

Possible recovery:

```text
Use Android storage API instead of Linux shell
```

Then retry.

---

# 42. Example: Development Task

User:

> "Build this Android project."

Agent:

```text
Inspecting project...

Detected:
Gradle project
Java/Kotlin
Android plugin
```

Runs build.

Error:

```text
SDK platform not found
```

Agent investigates:

```text
Required SDK:
Android 35

Installed:
Android 34
```

Then:

```text
Android SDK 35 is missing.

Install it?

[Install]
[Cancel]
```

After approval:

```text
Installing...
Verifying...
Re-running Gradle...
```

If another error appears, the same recovery loop continues.

---

# 43. Example: Python Project

User:

> "Run this Python project."

Agent:

```text
Inspecting project...

requirements.txt found.
```

Runs.

Error:

```text
ModuleNotFoundError: requests
```

Agent:

```text
Dependency missing.

Checking project dependency configuration...
```

Then installs the dependency in the appropriate environment if allowed.

Retries.

Then verifies the program's output.

---

# 44. Example: Android APK

User:

> "Install this APK."

Agent checks:

```text
Package name
Version
Architecture
Signature
Existing installation
Permissions
Available storage
```

If installation fails:

```text
INSTALL_FAILED_VERSION_DOWNGRADE
```

Agent explains:

> The installed version is newer than this APK.

Then offers safe options rather than blindly forcing an installation.

---

# 45. Example: Root Task

User:

> "Show me what's using the most CPU."

No root required.

Agent uses normal system information.

User:

> "Stop this system process."

Agent determines whether root is required.

Then:

```text
Root access is required.

Process:
example

Risk:
High

[Cancel]
[Enable Root Operation]
```

It should never silently escalate privileges.

---

# 46. User Interaction Modes

Provide three modes.

## Normal

The AI asks for important confirmations.

## Autonomous

The user grants predefined capabilities.

Example:

```text
Allow:
✓ File copy
✓ File conversion
✓ Package installation
✓ Rename

Always ask:
✓ Delete
✓ Root
✓ System modifications
```

## Manual

The AI produces plans and commands but does not execute them.

Useful for advanced users.

---

# 47. Command Preview

Before executing shell commands:

```text
Command Preview

Working directory:
/project

Command:
python build.py

Environment changes:
none

Risk:
Low

[Run]
[Edit]
[Cancel]
```

For batch operations, show a summarized preview.

---

# 48. Smart Alternatives

The agent should search for alternative methods when the preferred method fails.

Example:

```text
Method A:
FFmpeg

Failed:
unsupported codec

Alternative:
GStreamer

Available:
yes

Use alternative?
```

Or:

```text
Method A:
Android API

Unavailable

Method B:
Linux utility

Available

Switching to Method B.
```

---

# 49. Dependency Graph

Complex tasks may require multiple dependencies.

Example:

```text
Task
 │
 ├── Python
 │    └── requests
 │
 ├── FFmpeg
 │
 └── ImageMagick
```

The agent should install and verify dependencies in the correct order.

---

# 50. Execution Isolation

Separate:

```text
Android App
Linux Userspace
Tool Executor
Root Backend
```

A tool should receive only the permissions and paths it needs.

This limits damage if the AI makes a mistake.

---

# 51. Sandboxing

Non-root Linux operations should ideally run inside a controlled userspace.

Potential restrictions:

- filesystem mounts
- network access
- process limits
- CPU limits
- memory limits
- storage quotas

Different tasks can receive different sandbox policies.

---

# 52. Security Policy Engine

The policy engine should evaluate:

```text
Who requested it?
What operation?
Which files?
Which permissions?
Which privilege level?
How many files?
Is it destructive?
Is it reversible?
Does it affect the OS?
Does it require root?
```

Then calculate risk.

---

# 53. Audit Trail

For every meaningful operation:

```text
Timestamp
User request
AI plan
Tool
Arguments
Permission decision
Result
Verification
Recovery actions
Final state
```

The user can inspect the history.

---

# 54. Resource Limits

The agent should support limits such as:

```text
Maximum files per task
Maximum storage modification
Maximum CPU time
Maximum memory
Maximum network usage
Maximum recovery attempts
Maximum root operations
Maximum AI requests / cost per task
```

This protects against runaway plans.

---

# 55. Smart Scheduling

The agent can delay tasks.

Example:

> "Convert all these videos tonight."

The scheduler can wait until:

```text
charging = true
battery > threshold
device idle
storage available
```

Then execute.

---

# 56. User's VOA Example

VOA is an excellent demonstration but not the product itself.

Example input:

> "Process my VOA lessons. Keep Conversation MP3s, extract audio from Pronunciation and Speaking, ignore Main, and put everything in one folder."

The general agent discovers:

```text
Files
+
MP4 media
+
MP3 audio
+
FFmpeg capability
```

Then performs the task.

This demonstrates the architecture because the same system can later process completely unrelated tasks.

---

# 57. Killer Product Demonstration

A strong demo would show several unrelated tasks:

### Task 1

> "Organize my Downloads."

### Task 2

> "Convert these videos to MP3."

### Task 3

> "Install Python and run this project."

### Task 4

> "Why did this command fail?"

### Task 5

> "Find what's taking up my storage."

### Task 6

> "Build this Android project."

### Task 7

> "I have root. Clean these old files."

The same AI agent handles all of them using different tools.

---

# 58. Android Architecture

Recommended stack:

```text
Kotlin
Jetpack Compose
MVVM / Clean Architecture
Room
WorkManager / Foreground execution
Storage Access Framework
MediaStore
Linux userspace
Package manager
FFmpeg
Optional root backend
```

Core modules:

```text
app
agent
planner
policy
tools
executor
verification
recovery
linux
packages
media
files
android
root
scheduler
history
memory
security
```

---

# 59. Suggested Project Structure

```text
app/
├── agent/
│   ├── AgentEngine
│   ├── Planner
│   ├── ToolSelector
│   ├── ContextManager
│   └── ModelProvider
│
├── tools/
│   ├── files/
│   ├── media/
│   ├── linux/
│   ├── android/
│   ├── packages/
│   └── root/
│
├── execution/
│   ├── Executor
│   ├── ProcessManager
│   ├── Sandbox
│   └── Environment
│
├── recovery/
│   ├── ErrorAnalyzer
│   ├── DiagnosisEngine
│   ├── RecoveryPlanner
│   └── RetryManager
│
├── verification/
│   ├── FileVerifier
│   ├── MediaVerifier
│   ├── PackageVerifier
│   └── TaskVerifier
│
├── security/
│   ├── PolicyEngine
│   ├── PermissionManager
│   ├── RiskEngine
│   └── AuditLog
│
├── linux/
├── packages/
├── scheduler/
├── history/
├── memory/
└── ui/
```

---

# 60. Development Roadmap

## Phase 1 — MVP

Build:

- Chat interface
- Gemini integration with secure API-key storage and model selection
- File browsing
- File search
- Copy
- Move
- Rename
- Create folders
- Natural-language planning
- Tool execution
- Basic verification

## Phase 2 — Media

Add:

- FFmpeg
- Media probing
- Audio extraction
- Conversion
- Compression
- Batch processing

## Phase 3 — Error Recovery

Add:

- stdout/stderr capture
- Error classification
- Diagnosis
- Recovery planning
- Retry
- Verification
- Recovery history

This phase is one of the most important differentiators.

## Phase 4 — Linux Runtime

Add:

- Linux userspace
- Shell
- Package manager
- Python
- Git
- Common utilities

## Phase 5 — Package Intelligence

Add:

- Dependency detection
- Automatic package discovery
- Install/repair
- Package verification

## Phase 6 — Android Tools

Add:

- Apps
- Intents
- permissions
- APK management
- system information
- settings shortcuts

## Phase 7 — Root Backend

Add:

- root detection
- typed root tools
- policy engine
- confirmation
- audit logging

## Phase 8 — Advanced Agent

Add:

- scheduling
- autonomous mode
- multi-step workflows
- long-running tasks
- checkpoints
- plugins
- advanced recovery
- additional AI providers and automatic model routing

---

# 61. What Makes This Product Different?

A normal terminal:

```text
User knows command
        ↓
Terminal executes command
```

A normal automation app:

```text
Predefined workflow
        ↓
Execute
```

A simple AI command generator:

```text
User
 ↓
AI generates command
 ↓
User executes it
```

Your product:

```text
User describes goal
        ↓
AI understands goal
        ↓
AI inspects environment
        ↓
AI creates plan
        ↓
Policy checks plan
        ↓
Tools execute
        ↓
Results are observed
        ↓
AI diagnoses problems
        ↓
AI adapts strategy
        ↓
Tools execute again
        ↓
Results are verified
        ↓
Task completes
```

That is the central product differentiation.

---

# 62. The Agent Should Be Able to Say "I Don't Know"

A good agent should not hallucinate a solution.

If it cannot determine the cause:

```text
I could not confidently identify the cause.

I collected:
- command output
- error output
- environment information
- permissions
- file state

I need one of:
- additional permission
- Internet access
- user decision
- developer/debug information
```

Honest failure is better than dangerous guessing.

---

# 63. The Agent Should Explain Its Actions

Not hidden chain-of-thought.

Instead, provide concise operational reasoning:

```text
I found 3 possible causes.

Most likely:
the destination is not writable.

I'll check permissions before changing anything.
```

Then:

```text
Permission check complete.

The destination is read-only.

I'll use the Android storage API instead.
```

This gives transparency without exposing private internal reasoning.

---

# 64. The Agent Should Learn the Environment

Over time it can build a capability map:

```text
Device
 ├── Android 14
 ├── ARM64
 ├── Root available
 ├── Linux runtime installed
 ├── FFmpeg installed
 ├── Python installed
 ├── Git installed
 ├── 128 GB free
 └── /Download accessible
```

Then future tasks become faster.

---

# 65. General Workflow Example

User:

> "Take these 500 videos and make smaller versions."

Agent:

```text
Inspecting...
```

Finds:

```text
500 videos
Total: 87 GB
Free space: 12 GB
```

It realizes that processing everything simultaneously could exceed storage.

Plan:

```text
Process one video at a time.
Use temporary files.
Delete temporary file after verification.
Write output to selected destination.
Resume from checkpoints.
```

During processing:

```text
Video 87 failed.
```

Investigation:

```text
Unsupported audio codec.
```

Recovery:

```text
Try audio stream copy.
```

Verification:

```text
Output valid.
```

Continue:

```text
88 / 500
```

This is the behavior the product should target.

---

# 66. Natural Language Should Be the Primary Interface

The user should not need to know:

- Bash
- Linux
- FFmpeg
- Python
- package managers
- Android APIs
- root commands

But advanced users should still be able to open:

```text
Plan
Commands
Tool calls
Logs
Environment
```

and take control.

So the product serves both:

```text
Beginner ← AI abstraction → Advanced user
```

---

# 67. Final Product Vision

The final product should feel like:

> **"A computer operator living inside my Android phone."**

You tell it:

> "Do this."

It:

```text
understands
→ inspects
→ plans
→ checks permissions
→ selects tools
→ executes
→ observes
→ investigates
→ fixes
→ retries
→ verifies
→ completes
```

It can use:

```text
Android APIs
Linux
Shell
Packages
FFmpeg
Python
Git
Media tools
File tools
Root tools
Plugins
Cloud AI (user-selected provider and model)
```

But none of those are the product by themselves.

**The AI agent is the product.**

The tools are its capabilities.

The policy engine is its safety layer.

The recovery engine is its resilience.

The verification engine is what prevents false success.

The Linux environment gives it a broad technical toolbox.

Root gives it optional additional capabilities.

And natural language gives the user a simple interface to all of it.

---

# 68. One-Sentence Product Definition

> **A general-purpose Android AI agent that can operate the device through controlled tools, reason about failures, automatically recover when safe, verify its work, and use Android APIs, Linux packages, media utilities, optional root capabilities, and a user-selected cloud AI model—starting with Gemini—to accomplish the user's goal.**

---

# 69. Initial AI Implementation Decision

The first production AI integration will be **Google Gemini**.

This is an implementation choice, not a permanent architectural dependency. The application should launch with a polished Gemini experience while keeping the provider/model abstraction ready for additional cloud providers.

Initial priority order:

1. Gemini API integration
2. Secure user API-key storage
3. Gemini model selection
4. Tool calling / structured output
5. Agent execution + verification loop
6. Error diagnosis and recovery
7. Usage/error monitoring
8. Provider/model abstraction for future integrations

The user remains in control of the API key, selected model, privacy settings, permissions, and dangerous operations.
