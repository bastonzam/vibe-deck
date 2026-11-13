# สร้างและเผยแพร่ Release APK

## รุ่นแรก

- versionName: `0.2.0`
- versionCode: `2`
- package: `app.vibedeck`
- Build: `release`, ไม่ debuggable, เปิด R8 minification
- เป้าหมายแจกไฟล์: GitHub Release tag `v0.2.0`

## กุญแจเซ็น

เครื่องผู้ดูแลปัจจุบันมีกุญแจที่ `signing/vibedeck-release.jks` และค่าที่ Gradle อ่านจาก `keystore.properties` ไฟล์ทั้งสองไม่อยู่ใน Git ให้สำรองกุญแจและค่าลับไว้ในที่จัดเก็บส่วนตัวที่กู้คืนได้ การอัปเดต APK ผ่านช่องทางเดิมต้องใช้กุญแจเดิม

Release certificate fingerprints (เป็นข้อมูลสาธารณะ):

```text
SHA-1
A1:2A:AC:7D:12:B0:FE:6B:71:8A:46:E9:EE:2F:D3:E9:52:EE:33:E1

SHA-256
3F:AD:61:CB:9E:20:31:91:48:C6:44:0A:F4:61:0E:6B:69:21:5C:0D:54:27:61:5D:0F:F7:44:6C:FD:B6:24:DF
```

เพิ่ม Release SHA-1 ใน Firebase Project settings → Android app `app.vibedeck` → Add fingerprint แล้วดาวน์โหลด `google-services.json` ใหม่มาวางใน `app/` จึงจะใช้ Google Sign-In จาก APK ที่เซ็นด้วยกุญแจนี้ได้ ไม่ต้องเพิ่ม fingerprint แยกตามมือถือ

Release key ต่างจาก Debug key: Android อาจไม่ยอมติดตั้ง Release ทับ Debug เดิม ผู้พัฒนาที่ติดตั้ง Debug อยู่ควรสำรองข้อมูลเกมบนเครื่องก่อนเปลี่ยนรุ่น อย่าถอนติดตั้งแอปที่มี Guest save ซึ่งยังต้องเก็บไว้โดยไม่ได้สำรอง

## เตรียมเครื่องใหม่

1. ตั้ง SDK Platform 35, JDK 17+ และ `local.properties`
2. วาง Firebase config สำหรับโปรเจกต์ที่ต้องการใน `app/google-services.json`
3. นำ Release keystore เดิมจากที่สำรองไว้กลับมา หรือสร้างกุญแจสำหรับแอป fork ของตนเอง
4. สร้าง `keystore.properties` ที่ root ตามรูปแบบนี้ (ใช้ค่าของตนเอง):

   ```properties
   storeFile=signing/vibedeck-release.jks
   storePassword=YOUR_STORE_PASSWORD
   keyAlias=vibedeck
   keyPassword=YOUR_KEY_PASSWORD
   ```

Debug build ไม่ต้องใช้ไฟล์ signing ส่วน Release จะหยุดพร้อมข้อความแจ้งหากยังไม่ได้ตั้งค่า signing

## ตรวจและบิลด์

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/test-core.ps1
node --test tests/display.test.cjs
.\gradlew.bat :app:assembleRelease :app:lintRelease
```

Android Studio มี shared run configuration **VibeDeck Release APK** สำหรับรัน assembleRelease และ lintRelease ผ่าน Gradle

ผลลัพธ์ปกติ:

- APK: `app/build/outputs/apk/release/app-release.apk`
- R8 mapping: `app/build/outputs/mapping/release/mapping.txt`
- Lint report: `app/build/reports/lint-results-release.html`

เก็บ R8 mapping แยกตามรุ่นสำหรับอ่าน crash logs ส่วน APK ที่แจกควรตรวจ signature และ version ก่อนเผยแพร่

## GitHub Release

1. Commit source และเอกสารที่ตรวจแล้วลง `main`
2. สร้าง tag `v0.2.0` ตรงกับ commit ที่บิลด์
3. สร้าง GitHub Release โดยใช้ [release notes](releases/v0.2.0.md)
4. แนบ `vibedeck-0.2.0.apk` และ `vibedeck-0.2.0.apk.sha256`
5. ตรวจดาวน์โหลดไฟล์และ checksum หลังเผยแพร่

ตัวอย่างสำหรับเครื่องที่มี GitHub CLI และล็อกอินแล้ว:

```powershell
gh release create v0.2.0 dist/vibedeck-0.2.0.apk dist/vibedeck-0.2.0.apk.sha256 --repo bastonzam/vibedeck-android --target main --title "VibeDeck 0.2.0" --notes-file docs/releases/v0.2.0.md
```

อย่าแนบ `keystore.properties`, keystore, รหัสผ่าน, service-account credentials หรือไฟล์ backup ของ session ใน release assets
