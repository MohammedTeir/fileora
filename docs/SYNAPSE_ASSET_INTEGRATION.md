# Synapse v3 asset integration

All 38 supplied SVG concepts are installed as Android VectorDrawables. Their paths, strokes, circular/elliptical primitives, and rounded rectangle arcs are preserved by `scripts/import-v3-icons.py`. No concept requires a bitmap fallback. Originals remain under `assets/synapse/v3/icons/svg/`.

The package describes these as reconstructed concepts, not original exact vectors. Runtime use is listed below; decorative icons follow the selected accent and light/dark semantic colors.

| Concept | Source SVG | Runtime drawable | Compose integration |
|---|---|---|---|
| about | [SVG](../assets/synapse/v3/icons/svg/about.svg) | `synapse_icon_about.xml` | `AppearanceDesign`, `AboutDesign`, `PackageDetailDemo`, `DesignTabs` |
| activity | [SVG](../assets/synapse/v3/icons/svg/activity.svg) | `synapse_icon_activity.xml` | `PermissionsDesign`, `PackageDetailDemo`, `DesignTabs`, `ScreenDirectory`, `AgentDemo`, `MonitorDemo`, `AuditDemo`, `UsageCatalogDemo` |
| agent | [SVG](../assets/synapse/v3/icons/svg/agent.svg) | `synapse_icon_agent.xml` | `AboutDesign`, `ScreenDirectory`, `WelcomeDemo`, `TaskResultDesign` |
| appearance | [SVG](../assets/synapse/v3/icons/svg/appearance.svg) | `synapse_icon_appearance.xml` | `AppearanceDesign`, `DesignTabs` |
| approval | [SVG](../assets/synapse/v3/icons/svg/approval.svg) | `synapse_icon_approval.xml` | `PackageDetailDemo`, `AgentDemo`, `MonitorDemo`, `ActivityDemo`, `SecurityCatalogDemo` |
| apps | [SVG](../assets/synapse/v3/icons/svg/apps.svg) | `synapse_icon_apps.xml` | `AppsDemo`, `ToolsDemo`, `DesignTabs`, `AgentDemo`, `AttachmentsDemo` |
| audit | [SVG](../assets/synapse/v3/icons/svg/audit.svg) | `synapse_icon_audit.xml` | `PlanSummary`, `PlannedSteps`, `MonitorDemo`, `SecurityCatalogDemo`, `TaskResultDesign` |
| checkpoints | [SVG](../assets/synapse/v3/icons/svg/checkpoints.svg) | `synapse_icon_checkpoints.xml` | `DesignTabs`, `CheckpointDemo` |
| credentials | [SVG](../assets/synapse/v3/icons/svg/credentials.svg) | `synapse_icon_credentials.xml` | `CredentialCatalogDemo`, `DesignTabs` |
| delete | [SVG](../assets/synapse/v3/icons/svg/delete.svg) | `synapse_icon_delete.xml` | `CredentialCatalogDemo`, `AttachmentsDemo`, `PrivacyCatalogDemo`, `SecurityCatalogDemo` |
| device | [SVG](../assets/synapse/v3/icons/svg/device.svg) | `synapse_icon_device.xml` | `PermissionsDesign`, `AppearanceDesign`, `DeviceOverviewDemo`, `ToolsDemo`, `DesignTabs`, `ScreenDirectory`, `WelcomeDemo`, `MemoryCatalogDemo` |
| download | [SVG](../assets/synapse/v3/icons/svg/download.svg) | `synapse_icon_download.xml` | `AboutDesign`, `PackageDetailDemo` |
| file | [SVG](../assets/synapse/v3/icons/svg/file.svg) | `synapse_icon_file.xml` | `PermissionsDesign`, `AboutDesign`, `SandboxDemo`, `ToolsDemo`, `AttachmentsDemo`, `PlanSummary`, `PlanPermissions`, `MonitorDemo`, `RecoveryDemo`, `AuditDemo`, `UndoDemo`, `SecurityCatalogDemo`, `TaskResultDesign` |
| folder | [SVG](../assets/synapse/v3/icons/svg/folder.svg) | `synapse_icon_folder.xml` | `PermissionsDesign`, `SandboxDemo`, `ToolsDemo`, `DesignTabs`, `WelcomeDemo`, `AgentDemo`, `AttachmentsDemo`, `PlanSummary`, `PlanPermissions`, `MonitorDemo`, `AuditDemo`, `SecurityCatalogDemo`, `TaskResultDesign` |
| history | [SVG](../assets/synapse/v3/icons/svg/history.svg) | `synapse_icon_history.xml` | `TerminalDemo`, `DesignTabs`, `UsageCatalogDemo`, `MemoryCatalogDemo` |
| image | [SVG](../assets/synapse/v3/icons/svg/image.svg) | `synapse_icon_image.xml` | `PermissionsDesign`, `ToolsDemo`, `AttachmentsDemo` |
| memory | [SVG](../assets/synapse/v3/icons/svg/memory.svg) | `synapse_icon_memory.xml` | `DeviceOverviewDemo`, `DesignTabs`, `PrivacyCatalogDemo`, `MemoryCatalogDemo` |
| models | [SVG](../assets/synapse/v3/icons/svg/models.svg) | `synapse_icon_models.xml` | `AppearanceDesign`, `SettingsOverviewDemo`, `ModelCatalogDemo`, `DesignTabs` |
| network | [SVG](../assets/synapse/v3/icons/svg/network.svg) | `synapse_icon_network.xml` | `PermissionsDesign`, `AboutDesign`, `DeviceOverviewDemo`, `ToolsDemo` |
| packages | [SVG](../assets/synapse/v3/icons/svg/packages.svg) | `synapse_icon_packages.xml` | `PackagesDemo`, `PackageDetailDemo`, `ToolsDemo`, `DesignTabs`, `SecurityCatalogDemo` |
| pause | [SVG](../assets/synapse/v3/icons/svg/pause.svg) | `synapse_icon_pause.xml` | `ToolsDemo` |
| permissions | [SVG](../assets/synapse/v3/icons/svg/permissions.svg) | `synapse_icon_permissions.xml` | `PermissionsDesign`, `AppearanceDesign`, `DesignTabs`, `PlanPermissions`, `SecurityCatalogDemo` |
| play | [SVG](../assets/synapse/v3/icons/svg/play.svg) | `synapse_icon_play.xml` | `ToolsDemo`, `AgentDemo`, `PlanSummary`, `MonitorDemo`, `CheckpointDemo`, `ActivityDemo`, `UndoDemo` |
| plus | [SVG](../assets/synapse/v3/icons/svg/plus.svg) | `synapse_icon_plus.xml` | `CheckpointDemo` |
| privacy | [SVG](../assets/synapse/v3/icons/svg/privacy.svg) | `synapse_icon_privacy.xml` | `PermissionsDesign`, `AboutDesign`, `DesignTabs`, `PrivacyCatalogDemo` |
| providers | [SVG](../assets/synapse/v3/icons/svg/providers.svg) | `synapse_icon_providers.xml` | `ToolsDemo`, `SettingsOverviewDemo`, `ProviderCatalogDemo`, `CredentialCatalogDemo`, `ModelCatalogDemo`, `DesignTabs`, `WelcomeDemo`, `UsageCatalogDemo` |
| recovery | [SVG](../assets/synapse/v3/icons/svg/recovery.svg) | `synapse_icon_recovery.xml` | `ToolsDemo`, `RecoveryDemo`, `UndoDemo` |
| root | [SVG](../assets/synapse/v3/icons/svg/root.svg) | `synapse_icon_root.xml` | `SecurityCatalogDemo` |
| schedules | [SVG](../assets/synapse/v3/icons/svg/schedules.svg) | `synapse_icon_schedules.xml` | `PermissionsDesign`, `DesignTabs`, `AgentDemo`, `ScheduleDemo` |
| search | [SVG](../assets/synapse/v3/icons/svg/search.svg) | `synapse_icon_search.xml` | `DesignSearch`, `AttachmentsDemo` |
| security | [SVG](../assets/synapse/v3/icons/svg/security.svg) | `synapse_icon_security.xml` | `CredentialCatalogDemo`, `DesignTabs`, `WelcomeDemo`, `PlanSummary`, `PlanPermissions`, `MonitorDemo`, `AuditDemo`, `UndoDemo`, `UsageCatalogDemo`, `SecurityCatalogDemo` |
| settings | [SVG](../assets/synapse/v3/icons/svg/settings.svg) | `synapse_icon_settings.xml` | `PermissionsDesign`, `DeviceOverviewDemo`, `ProviderCatalogDemo`, `CredentialCatalogDemo`, `SynapseWorkspace`, `ScreenDirectory`, `WelcomeDemo`, `CheckpointDemo`, `PrivacyCatalogDemo`, `MemoryCatalogDemo` |
| terminal | [SVG](../assets/synapse/v3/icons/svg/terminal.svg) | `synapse_icon_terminal.xml` | `AboutDesign`, `PackageDetailDemo`, `TerminalDemo`, `ToolsDemo`, `DesignTabs`, `AgentDemo`, `AuditDemo`, `SecurityCatalogDemo` |
| tools | [SVG](../assets/synapse/v3/icons/svg/tools.svg) | `synapse_icon_tools.xml` | `DeviceOverviewDemo`, `AppsDemo`, `PackagesDemo`, `TerminalDemo`, `ToolsDemo`, `DesignTabs`, `AgentDemo`, `TaskResultDesign` |
| undo | [SVG](../assets/synapse/v3/icons/svg/undo.svg) | `synapse_icon_undo.xml` | `UndoDemo`, `TaskResultDesign` |
| upload | [SVG](../assets/synapse/v3/icons/svg/upload.svg) | `synapse_icon_upload.xml` | `AttachmentsDemo`, `PrivacyCatalogDemo` |
| usage | [SVG](../assets/synapse/v3/icons/svg/usage.svg) | `synapse_icon_usage.xml` | `SettingsOverviewDemo`, `DesignTabs`, `UsageCatalogDemo` |
| warning | [SVG](../assets/synapse/v3/icons/svg/warning.svg) | `synapse_icon_warning.xml` | `ActivityDemo`, `SecurityCatalogDemo` |

## Branding

- Header, Welcome, About, and Compose splash use the supplied `brand/png/synapse-mark-extracted-256.png` byte-for-byte as `drawable-nodpi/synapse_brand_mark.png`.
- Adaptive foreground, monochrome vector, standard/round XML, and five launcher density PNGs are copied from `assets/synapse/v3/android/res/`. Runtime foreground is in `drawable-nodpi/` to prevent unintended density scaling.
- Foreground alpha bounds: `(137,136)–(296,284)` in a 432×432 layer. All visible pixels fit inside the 66dp safe circle of the 108dp layer. [Mask review](visual-parity/launcher-masks.png) covers full layer, circle, and rounded square. This is a geometry check, not certification on every launcher.
- [Android icon catalog](visual-parity/icon-catalog.png) renders all 38 runtime vectors through Compose/native Android canvas.
- Earlier official launcher PNG remains in runtime resources, unused, and is reported by lint. Source artwork is preserved.

## Availability and packaging

All 31 `references/screen_01.png` through `references/screen_31.png` paths exist. No reference screenshot or SVG source is packaged in any of the five debug APKs.

No separately usable Welcome phone illustration, device photograph, media thumbnail set, provider logos, or third-party app/package logos are supplied or named in the v3 manifest. Repository asset/import/archive searches found no separate source package for those illustrations. Their presence inside a reference screenshot does not supply a standalone asset. No screenshot fragments have been extracted as runtime UI.

The legacy asset policy is superseded by the approved v3 integration described in the root `AGENTS.md`.
