**🧙‍♂️ Go Rotation 90 Xposed Module Project.**

### GoRotation90

이 Xposed 모듈은 루팅된 LG Wing 스마트폰의 세컨드 런처 화면의 회전기능을 추가합니다.

윙의 두 번째 런처인 `com.lge.secondlauncher`의 로테이션 설정을 바꿔치기 하기 때문에 예기치 않은 부작용을 일으킬 수 있습니다.

성능 영향: updateRotationUnchecked 메서드가 호출될 때마다 추가 로직이 실행되므로, 미세하지만 성능에 영향을 줄 수 있습니다.

배터리 소모: 지속적인 회전 체크와 설정 변경으로 인해 배터리 소모가 증가할 수 있습니다.

호환성 문제: 일부 앱들은 특정 화면 방향을 강제로 요구할 수 있는데, 이 모듈이 그러한 요구를 무시할 수 있습니다.

이 모듈은 LG Wing에 특화되어 있어, 다른 기기에서는 제대로 작동하지 않거나 문제를 일으킬 수 있습니다.

This Xposed module adds rotation functionality to the second launcher screen of a rooted LG Wing smartphone.

By overriding the rotation settings of Wing's second launcher, com.lge.secondlauncher, it may cause unexpected side effects.

Performance impact: Additional logic is executed every time the updateRotationUnchecked method is called, which may have a slight impact on performance.

Battery consumption: Continuous rotation checks and setting changes may increase battery consumption.

Compatibility issues: Some apps may forcefully require a specific screen orientation, which this module might override.

This module is specifically designed for the LG Wing and may not function properly or cause issues on other devices.
