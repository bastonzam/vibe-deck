# ผลการตรวจ 13 พฤศจิกายน 2026

- Android Studio build ใช้ AGP 8.7.0 / Gradle 8.9 / Kotlin 2.0.21 สำเร็จที่ `:app:assembleDebug` ได้ APK ที่ `app/build/outputs/apk/debug/app-debug.apk`
- APK รุ่นสุดท้ายสร้างหลัง source Kotlin ที่แก้ล่าสุด ตรวจ ZIP แล้วมี AndroidManifest, DEX 14 ไฟล์ และ launcher icon 10 resources
- Kotlin + Compose + Firebase sources ผ่าน compiler 2.0.21 โดยใช้ dependencies ที่ cache ไว้จริง และผ่าน compileDebugKotlin ใน Gradle build
- `scripts/test-core.ps1`: ผ่าน 12 tests ครอบคลุม canonical URL/game ID, metadata priorities/relative images/fallback/text limits, rating add/edit/delete และ private/loopback/link-local/IPv6 address rejection
- `node --test tests/display.test.cjs`: ผ่าน 7 tests สำหรับ metadata orientation/manifest precedence, SDK request, Safe Area fallback และ browser unlock
- `tests/firestore/rules.test.mjs`: syntax check ผ่าน; ยัง **ไม่ได้รัน Emulator tests** เนื่องจากไม่มี Firebase CLI ใน cache และสภาพแวดล้อมคำสั่งดาวน์โหลดผ่านเครือข่ายไม่ได้ มีวิธีรันใน FIREBASE_SETUP.md ต้องตรวจ Rules ด้วย Emulator ก่อน deploy จริง
- Launcher PNG ครบ 11 ไฟล์ ตรวจขนาด 48/72/96/144/192 และ 512 px; 512 px เป็น opaque RGB PNG, round เป็น alpha circle ตรวจภาพแล้ว
- `google-services.json`: ตรวจ package ตรง `app.vibedeck` และมี Web OAuth client แล้ว ไม่ได้พิมพ์หรือย้าย API key / client ID ลงซอร์ส
- Store Compose Preview render ได้ใน Android Studio ยังไม่ได้ทดสอบ login หรือเขียนข้อมูลกับ Firebase production และยังไม่ได้ทดสอบทุก flow บนอุปกรณ์จริง
- ไม่ได้รัน lintDebug จนเสร็จ: CLI ที่จำกัดสิทธิ์เข้าถึง SDK ไม่ได้ จึงใช้ native Android Studio ทำ assembleDebug สำเร็จแทน ไม่มีการเปลี่ยนเวอร์ชัน build tools หรือ deploy Rules อัตโนมัติ

ข้อมูลตัวอย่าง 14 เกมใช้สำหรับ Preview/Fake เท่านั้น Runtime ใช้ FirestoreGameRepository จริง รูปเป็น hotlink และไม่มี moderation UI ข้อจำกัดและขั้นตอน Console/deploy อยู่ใน [FIREBASE_SETUP.md](FIREBASE_SETUP.md)
