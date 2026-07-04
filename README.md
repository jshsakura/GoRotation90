<div align="center">
  <img src="https://github.com/jshsakura/GoRotation90/blob/main/app/sampledata/gocat90.png?raw=true" width="180" style="border-radius: 20px;"/>
</div>

**🧙‍♂️ Go Rotation 90 Xposed Module Project.**

### GoRotation90

이 Xposed 모듈은 루팅된 LG Wing 스마트폰에서 **자동회전 토글을 회전 트리거**로 사용해 화면 방향을 강제합니다.

- 자동회전 **OFF** → 가로(90°) 고정
- 자동회전 **ON** → 세로(0°) 고정

시스템 프레임워크의 `DisplayRotation.rotationForOrientation`을 후킹해 회전 계산 결과를 토글 상태에 따라 강제하고, 토글이 바뀌는 순간 ContentObserver가 회전 재평가를 즉시 트리거하므로 지연 없이 바로 회전됩니다.

동작 방식은 전부 **이벤트 기반**입니다. 폴링이나 지속적인 체크는 없으며, 회전 이벤트가 발생할 때만 후킹 로직이 실행되고 설정값 읽기도 SettingsProvider 클라이언트 캐시를 사용하므로 성능·배터리 영향은 사실상 없습니다.

#### 설치

1. APK 설치 ([Releases](https://github.com/jshsakura/GoRotation90/releases)에서 다운로드)
2. LSPosed에서 모듈 활성화 — 스코프는 **System Framework(android)** 만 필요
3. 재부팅

#### 주의 사항

- 회전을 시스템 차원에서 강제하므로, 특정 방향을 요구하는 앱의 요청을 무시할 수 있습니다.
- 이 모듈은 LG Wing에 특화되어 있어 다른 기기에서는 제대로 작동하지 않거나 문제를 일으킬 수 있습니다.

---

This Xposed module for rooted LG Wing smartphones uses the **auto-rotate toggle as a rotation trigger** to force the screen orientation.

- Auto-rotate **OFF** → locked to landscape (90°)
- Auto-rotate **ON** → locked to portrait (0°)

It hooks `DisplayRotation.rotationForOrientation` in the system framework to force the computed rotation based on the toggle state, and a ContentObserver triggers an immediate rotation re-evaluation the moment the toggle changes, so the screen rotates without delay.

Everything is **event-driven**. There is no polling or continuous checking — the hook logic only runs on rotation events, and settings reads hit the SettingsProvider client-side cache, so the performance and battery impact is effectively zero.

#### Installation

1. Install the APK (download from [Releases](https://github.com/jshsakura/GoRotation90/releases))
2. Enable the module in LSPosed — only the **System Framework (android)** scope is required
3. Reboot

#### Caveats

- Since rotation is forced at the system level, apps that require a specific orientation may have their requests overridden.
- This module is specifically designed for the LG Wing and may not function properly or cause issues on other devices.
