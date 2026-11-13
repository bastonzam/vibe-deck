<p align="center">
  <img src="app/src/main/res/ic_launcher-web.png" width="112" alt="VibeDeck app icon" />
</p>

# VibeDeck

**ค้นพบ เล่น และแบ่งปันเกมเว็บที่สร้างจากจินตนาการของชุมชน**

VibeDeck เป็นแอป Android สำหรับรวมเกมเว็บจากผู้สร้าง โดยเฉพาะเกมที่พัฒนาด้วยเครื่องมือ AI และแนวทาง vibe coding ผู้เล่นเปิดเกมจากลิงก์ได้ทันทีผ่าน WebView เต็มจอ เก็บเกมที่ชอบ และร่วมให้คะแนนหรือคอมเมนต์โดยไม่ต้องติดตั้งแต่ละเกมแยกกัน

[ดาวน์โหลด APK ล่าสุด](https://github.com/bastonzam/vibedeck-android/releases/latest) · [ตั้งค่า Firebase](docs/FIREBASE_SETUP.md) · [สร้าง Release](docs/RELEASING.md) · [โครงสร้าง UI](docs/UI_GUIDE.md)

## สำหรับผู้เล่น

### ค้นพบเกม

- **Store** แสดงเกมเป็นรายการอ่านง่ายเหมือน Library พร้อมไอคอนเล็ก ชื่อ แนวเกม คะแนน และปุ่มเล่น ไม่ต้องมีภาพปกขนาดใหญ่
- ดูเกม **มาใหม่**, **ยอดนิยม** และรายการคัดสรร พร้อมตัวกรอง Arcade, Puzzle, Idle, Platformer และ Strategy
- **Search** ค้นจากชื่อเกม ผู้สร้าง หรือเครื่องมือ AI และกรองแนวเกม/ทิศทางจอได้ พร้อมประวัติค้นหาบนเครื่อง
- หน้า **Game Detail** มีข้อมูลผู้สร้าง คำอธิบาย ทิศทางจอ รูปตัวอย่างถ้ามี และปุ่มเล่นที่อยู่ด้านล่างเสมอ
- รูปที่ไม่มีหรือโหลดไม่ได้ใช้ gradient และอักษรย่อของชื่อเกมแทน

### เปิดเกมจากลิงก์

1. กด **เพิ่มเกม** และวาง URL แบบ http/https
2. แอปอ่านชื่อ ภาพ และคำอธิบายจาก Open Graph, Twitter tags และ HTML title ให้โดยอัตโนมัติ
3. แก้ชื่อ คำอธิบาย รูป แท็ก หรือแนวจอก่อนบันทึกได้ ถ้าอ่าน metadata ไม่สำเร็จยังกรอกเองและเปิดเล่นได้
4. เลือก **เล่นเลย** ได้โดยไม่ต้องล็อกอิน หรือ **บันทึกเข้า Store** หลังล็อกอิน Google
5. ถ้า URL นี้มีเกมอยู่แล้ว จะแสดง **เกมนี้มีอยู่แล้ว** พร้อมปุ่มไปหน้าเกมเดิม และปิดปุ่มบันทึกซ้ำ

### เล่นอย่างต่อเนื่อง

- WebView เต็มจอและเปิด keep-screen-on เป็นค่าเริ่มต้น
- เลือก **Full Screen / Safe Area** และ **อัตโนมัติ / แนวนอน / แนวตั้ง** ที่คลัง → ปุ่มตั้งค่า
- Safe Area เว้นกล้อง รอยบาก และพื้นที่ระบบ พร้อมปรับตามการหมุนเครื่อง
- โหมดอัตโนมัติอ่านแนวจอจากเว็บหรือ Web App Manifest; ถ้าไม่มีค่าจะหมุนตามเซ็นเซอร์
- รายการ **ล่าสุด** และ **ที่ชอบ** เก็บบนเครื่อง ทั้งเกมใน Store และเกมที่เปิดจาก URL เอง
- Cookies, localStorage และ IndexedDB ใช้โปรไฟล์ WebView ถาวร เพื่อให้เว็บอ่านบัญชีหรือ Guest save เดิมได้

การกลับมาเล่นต่อขึ้นกับสิ่งที่เกมบันทึกไว้และอายุ session ของเว็บ แอปไม่ต่ออายุ session ที่เซิร์ฟเวอร์หมดอายุ และไม่กู้ความคืบหน้าที่เกมเก็บเฉพาะในหน่วยความจำ การล้างข้อมูลแอปหรือถอนติดตั้งจะลบข้อมูลในเครื่องด้วย

### บัญชีและชุมชน

| การกระทำ | ต้องล็อกอิน |
| --- | --- |
| ดูรายละเอียด ค้นหา และเล่นเกม | ไม่ต้อง |
| เก็บล่าสุด/ที่ชอบบนเครื่อง | ไม่ต้อง |
| บันทึกเกมเข้า Store | ต้อง |
| ให้ดาว คอมเมนต์ และรายงาน | ต้อง |

- Google Sign-In ใช้ **Credential Manager**
- ให้ดาว 1–5 ได้คนละหนึ่งคะแนนต่อเกม แก้ไขหรือลบได้ และดูคะแนนเฉลี่ยกับจำนวนโหวต
- คอมเมนต์ได้ไม่เกิน 500 ตัวอักษร โหลดใหม่ก่อนครั้งละ 20 รายการ ลบของตัวเองหรือรายงานของผู้อื่นได้
- แชร์เกมผ่าน Android Share Sheet และส่งรายงานเกมให้ทีมงานผ่าน Firestore
- ถ้า Google Login สำเร็จแต่บันทึกโปรไฟล์ไม่ผ่าน แอปแสดงสถานะ **เข้าสู่ระบบแล้ว** และแจ้งปัญหาโปรไฟล์แยก พร้อม retry โดยไม่เลือกบัญชีใหม่
- ถ้าโหลด Store ได้เพียงบางส่วน เกมที่โหลดได้ยังแสดงและเล่นได้ พร้อมข้อความอัปเดตเฉพาะส่วน ไม่แสดงเป็นความล้มเหลวทั้งหน้า

## สำหรับผู้พัฒนาเกมเว็บ

คัดลอก `sdk/vibedeck.js` ไปเสิร์ฟกับเกม แล้วเรียกจาก JavaScript ได้ทั้งในแอปและเบราว์เซอร์ทั่วไป

```html
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
<meta name="vibedeck-orientation" content="landscape">
<meta property="og:title" content="ชื่อเกมของคุณ">
<meta property="og:description" content="คำอธิบายเกมสั้น ๆ">
<meta property="og:image" content="https://your-domain.example/cover.png">
<script src="vibedeck.js"></script>
<button id="play">เริ่มเล่น</button>
<script>
  VibeDeck.keepScreenOn(true);
  VibeDeck.setOrientation('landscape');
  VibeDeck.setSafeArea(true);
  document.getElementById('play').addEventListener('click', function () {
    VibeDeck.haptic('light');
  });
</script>
```

| SDK | การใช้งาน |
| --- | --- |
| `VibeDeck.isNative` | ตรวจว่าเปิดอยู่ในแอปหรือไม่ |
| `VibeDeck.keepScreenOn(true)` | เปิด/ปิดการกันหน้าจอดับ |
| `VibeDeck.haptic('light')` | feedback เมื่อกดปุ่ม รองรับ light / medium / heavy |
| `VibeDeck.setOrientation('landscape')` | portrait / landscape / auto และ primary/secondary variants |
| `VibeDeck.setSafeArea(true)` | เปิด/ปิดการเว้นขอบเนื้อหา |

เรียก haptic โดยตรงได้ด้วย `window.VibeDeckNative.haptic('light')` ในแอป แต่แนะนำ SDK เพื่อรองรับ browser fallback การสั่นขึ้นกับฮาร์ดแวร์และการตั้งค่า feedback ของอุปกรณ์

ใน native Safe Area แอปย่อ viewport ให้แล้ว เว็บจึงไม่ต้องเพิ่ม padding ซ้ำ บนเบราว์เซอร์ SDK ใช้ CSS `env(safe-area-inset-*)`; เกมที่ใช้ fixed/canvas ควรจัด controls ให้รองรับเอง การล็อกแนวจอในเบราว์เซอร์อาจต้องเข้า fullscreen จากการแตะ

การล็อกอิน Google ของ VibeDeck กับบัญชีภายในเกมเป็นคนละ session รุ่นนี้ยังไม่เปิด API ส่ง token/SSO ข้ามโดเมนให้เกมทั่วไป

### Deep link

```text
vibedeck://play?url=https%3A%2F%2Fexample.com
```

```powershell
adb shell am start -a android.intent.action.VIEW -d "vibedeck://play?url=https%3A%2F%2Fexample.com"
```

## เทคโนโลยี

| ส่วน | เวอร์ชัน/เครื่องมือ |
| --- | --- |
| ภาษา | Kotlin 2.0.21 |
| UI | Jetpack Compose + Material3, Compose BoM 2024.10.01 |
| Build | AGP 8.7.0, Gradle Wrapper 8.9, JDK 17+ |
| Android | minSdk 24, compileSdk/targetSdk 35 |
| Navigation | navigation-compose 2.8.3 |
| ภาพ | Coil 2.7.0 |
| Backend | Firebase Authentication / Cloud Firestore, BoM 33.5.1 |
| Google Sign-In | Credential Manager 1.3.0, googleid 1.1.1 |
| Metadata | OkHttp 4.12.0 + Jsoup 1.18.1 |

## เริ่มพัฒนา

```powershell
git clone https://github.com/bastonzam/vibedeck-android.git
cd vibedeck-android
```

1. เปิดโฟลเดอร์ด้วย Android Studio และติดตั้ง Android SDK Platform 35
2. ตั้ง `sdk.dir` ใน `local.properties` ให้ตรงกับเครื่อง (Android Studio สร้างให้ได้)
3. เพิ่มแอป Android package **`app.vibedeck`** ใน Firebase ของคุณ
4. เปิด Google provider, สร้าง Firestore Database และเพิ่ม SHA-1 ของกุญแจที่ใช้เซ็นแอป
5. ดาวน์โหลด `google-services.json` วางที่ `app/google-services.json` ไฟล์นี้ไม่ได้เก็บใน Git
6. Deploy Rules และ indexes ตาม [FIREBASE_SETUP.md](docs/FIREBASE_SETUP.md) แล้ว Sync Gradle

```powershell
.\gradlew.bat :app:assembleDebug
```

Linux/macOS ใช้ `bash gradlew :app:assembleDebug` ไฟล์ Debug APK อยู่ที่ `app/build/outputs/apk/debug/app-debug.apk`

### Firebase Console ที่ต้องตั้งค่า

- **Authentication → Sign-in method → Google**: เปิดและระบุ support email
- **Project settings → Your apps → app.vibedeck → Add fingerprint**: เพิ่ม SHA-1 ของ Debug หรือ Release ให้ตรงกับ APK
- **Firestore Database**: สร้างฐานข้อมูลและเปิด Cloud Firestore API
- **Rules / Indexes**: deploy ไฟล์ใน repository
- Web client ID อ่านจาก resource `default_web_client_id` ที่ Google Services plugin สร้างให้ ไม่ hardcode ใน Kotlin

SHA-1 เป็นลายนิ้วมือของ **กุญแจเซ็นแอป ไม่ใช่มือถือ** APK ที่เซ็นด้วยกุญแจเดียวกันใช้ค่าเดียวกันทุกเครื่อง สำหรับ GitHub Release รุ่นแรกให้ลงทะเบียน Release SHA-1 ที่ระบุใน [RELEASING.md](docs/RELEASING.md) ด้วย

```powershell
firebase login
firebase deploy --only firestore --project YOUR_PROJECT_ID
```

## โครงสร้างโปรเจกต์

```text
app/src/main/java/app/vibedeck/
├── MainActivity.kt          # ทางเข้าแอป, play(url), deep link
├── GameActivity.kt          # WebView เต็มจอ, insets, session และ orientation
├── GameBridge.kt            # native bridge สำหรับเกม
├── RecentStore.kt           # ล่าสุดและที่ชอบบนเครื่อง
├── UrlPolicy.kt             # normalize/ตรวจ URL
├── auth/                    # Google Sign-In และ profile sync ที่แยกสถานะ
├── data/
│   ├── model/               # Game, drafts, ratings, comments, canonical game ID
│   ├── metadata/            # HTML parser, fetcher และ public-network policy
│   ├── remote/              # FirestoreGameRepository
│   ├── GameRepository.kt    # interface และ Fake สำหรับพัฒนา
│   └── CatalogRefresher.kt  # โหลดหลายแหล่ง, partial success และ stale-request guard
└── ui/
    ├── theme/               # สี, Typography และ dark/light theme
    ├── components/          # รายการเกม, artwork fallback, notices และ badges
    ├── store/               # หน้าแรกแบบรายการ
    ├── detail/              # รายละเอียด, rating และ comments
    ├── search/              # ค้นหาและตัวกรอง
    ├── library/             # ล่าสุด / ที่ชอบ
    └── add/                 # preview, แก้ข้อมูล และกันเกมซ้ำ
sdk/vibedeck.js
firestore.rules
firestore.indexes.json
scripts/
tests/
```

UI ใช้ ViewModel + StateFlow และ Repository interface ไม่มี Firestore calls ตรงจาก Composable Runtime ใช้ `FirestoreGameRepository` ส่วน `FakeGameRepository` และ 14 เกมตัวอย่างใช้สำหรับ Preview/การพัฒนา

### ความถูกต้องของข้อมูล

- game ID เป็น SHA-256 ของ canonical normalized URL: รวม host case/default port/root slash/dot path และตัด fragment เก็บ query และ path case ที่มีความหมายไว้
- Preview ตรวจเกมเดิมก่อนบันทึก และ transaction แบบ create-if-absent ตรวจซ้ำอีกครั้งเพื่อกันคำขอพร้อมกัน
- คะแนนรายคนและ ratingSum/ratingCount/ratingAvg เปลี่ยนใน transaction เดียว พร้อมตรวจส่วนต่างใน Rules
- คอมเมนต์กับ commentCount เปลี่ยนแบบ atomic; `commentMutationId` ใช้เชื่อมการแก้ counter กับเอกสารคอมเมนต์ใน Rules
- หน้า Detail ใช้ snapshot listeners สำหรับเกมและคะแนนของบัญชีปัจจุบัน และถอด listeners เมื่อออกจากหน้า
- Reports เป็น create-only สำหรับ client และไม่อนุญาตให้อ่านรายงานจากแอป

### Metadata และภาพ

Fetcher รับ http/https ผ่าน UrlPolicy, จำกัด HTML 1 MiB, timeout รวม 10 วินาที และ redirect สูงสุด 5 ครั้ง ตรวจ DNS รวม literal IP เพื่อปฏิเสธ localhost/private/link-local/IPv6 local และไม่ใช้ cookie ของ WebView ไปอ่าน metadata รูปเก็บเป็น hotlink โดยมี fallback เมื่อโหลดไม่ได้

## การทดสอบ

หลัง Gradle Sync ดาวน์โหลด dependencies แล้ว:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/test-core.ps1
node --test tests/display.test.cjs
```

ชุด Kotlin มี 27 tests: metadata/game ID/rating/network policy 12, login/profile sync 7 และ catalog partial success/concurrency/cancellation 8 ส่วน JavaScript SDK มี 7 tests

Rules tests ใช้ Firebase Emulator กับ demo project:

```powershell
cd tests/firestore
npm install
npm test
```

```powershell
.\gradlew.bat :app:lintDebug
```

ดูรายการทดสอบ TalkBack, font scale, หมุนจอ และหน้าจอต่าง ๆ ใน [UI_GUIDE.md](docs/UI_GUIDE.md)

## Release APK

ใช้ Release signing key ถาวรและ R8 minification ดูขั้นตอนทั้งหมดที่ [RELEASING.md](docs/RELEASING.md)

```powershell
.\gradlew.bat :app:assembleRelease :app:lintRelease
```

ไฟล์กุญแจ, รหัสผ่าน, `keystore.properties`, Firebase config, SDK path และ build outputs ถูกแยกออกจาก Git APK สำหรับผู้ใช้แจกผ่าน GitHub Releases แทนการเก็บ binary ใน source history

## สถานะและข้อจำกัด

- ยังไม่มี moderation dashboard, ระบบยืนยันผู้สร้าง หรือ abuse rate limiting; สร้างเกมเป็น approved ตามนโยบายของเฟสนี้
- Search ค้นจากชุด catalog ที่โหลดแล้ว (ล่าสุด/คะแนนสูงอย่างละ 100 และ query เพิ่มตาม tag) ยังไม่ใช่ full-text search ทั้งฐานข้อมูล
- รูป hotlink อาจหมดอายุหรือถูกเว็บกันไว้; image loader ปิด automatic redirects และใช้ gradient fallback
- Metadata ไม่รัน JavaScript เว็บที่สร้าง tags หลังรัน JS หรือต้อง login อาจต้องกรอกเอง
- Firestore อ่าน cache ได้ แต่ transaction ต้องเชื่อมต่อ backend; comment/report batch อาจรอ server acknowledgement
- การเขียนคะแนนรวมลงเอกสารเกมเดียวอาจมี contention เมื่อ traffic สูง
- Rules ตรวจรูปแบบ hash ID แต่ไม่คำนวณ SHA-256 ของ URL ให้เอง การบังคับ uniqueness กับ client ที่แก้แอปเองอย่างสมบูรณ์ต้องใช้ trusted backend
- SSO ระหว่างบัญชี VibeDeck กับเกมภายนอกยังเป็นงานต่อไป

## เอกสารเพิ่มเติม

- [Firebase, schema, security rules และ Console setup](docs/FIREBASE_SETUP.md)
- [Release signing และการแจก APK](docs/RELEASING.md)
- [UI architecture และ manual test checklist](docs/UI_GUIDE.md)
- [ประวัติการตรวจสอบ](docs/VERIFICATION.md)
