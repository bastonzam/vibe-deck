# Firebase สำหรับ VibeDeck

ใช้ Firebase BoM **33.5.1**, Google Services **4.4.2**, Credential Manager **1.3.0**, googleid **1.1.1**, OkHttp **4.12.0**, Jsoup **1.18.1** และ coroutines Tasks **1.8.1** คง AGP **8.7.0**, Gradle **8.9**, Kotlin **2.0.21** เดิม

## ตั้งค่า Console

1. ใน Firebase Console เลือกโปรเจกต์ของแอป Android ที่ package เป็น `app.vibedeck`
2. Authentication → Sign-in method → เปิด **Google** และระบุ support email
3. Project settings → Android app → เพิ่ม SHA-1 ของ debug key ใช้ผลจาก `gradle :app:signingReport` เมื่อ SDK พร้อม หากเผยแพร่ผ่าน Play ให้เพิ่ม SHA-1/SHA-256 ของ **Play App Signing** ด้วย
4. ดาวน์โหลด `google-services.json` ใหม่ วางที่ `app/google-services.json` แอปใช้ Google Services plugin สร้าง `default_web_client_id` จาก OAuth client ประเภท Web (type 3) ให้เอง ตรวจไฟล์ปัจจุบันแล้ว package ตรงและมี Web OAuth client โดยไม่ได้คัดลอกค่าเหล่านั้นลงซอร์ส
5. หากต้องกำหนด Web client ID เอง สร้าง resource ชื่อ `vibedeck_web_client_id` ใน `app/src/main/res/values/google_client_id.xml` เป็น fallback ห้ามใช้ Android OAuth client ID และอย่าสร้าง `default_web_client_id` ซ้ำกับ plugin:

   ```xml
   <resources>
       <string name="vibedeck_web_client_id" translatable="false">YOUR_WEB_CLIENT_ID.apps.googleusercontent.com</string>
   </resources>
   ```

6. Firestore Database → Create database → เลือก region ที่เหมาะสม → ใช้ Production mode และ deploy Rules ที่ให้มา อย่าเปิด allow-all rules
7. จาก root ของโปรเจกต์ หลังติดตั้ง Firebase CLI และ login แล้ว:

   ```powershell
   firebase use --add
   firebase deploy --only firestore
   ```

   หรือระบุ `--project YOUR_PROJECT_ID` โดยตรง รอ composite indexes สร้างเสร็จก่อนทดสอบ query ไม่มีการ deploy หรือเปิดแผนเสียเงินอัตโนมัติในงานนี้

## โครงสร้างและเส้นทางข้อมูล

- `auth/AuthRepository.kt`: Credential Manager → Google ID token → Firebase Auth → โปรไฟล์ `users/{uid}` ด้วย server timestamp รักษา createdAt เดิม ล้าง Firebase session และ credential state เมื่อออกจากระบบ
- สถานะเข้าสู่ระบบแยกจากการซิงค์โปรไฟล์ใน `auth/AuthSession.kt`: เมื่อ Firebase Auth สำเร็จ หน้าล็อกอินปิดได้ทันทีและงานที่รอล็อกอินทำต่อได้ การเขียน Firestore ทำเบื้องหลัง จำกัดเวลารอ 12 วินาที หากล้มเหลวแสดงข้อความ **เข้าสู่ระบบแล้ว แต่ยังบันทึกโปรไฟล์ไม่ได้** พร้อมปุ่ม retry โดยไม่เลือก Google account ใหม่ บัญชีที่ล็อกอินค้างอยู่จะลอง sync เมื่อเปิดแอปใหม่ และการส่งคอมเมนต์รอให้โปรไฟล์พร้อมตาม Rules
- `ui/AuthGateViewModel.kt`: เก็บการกระทำรอ login sheet และทำต่ออัตโนมัติเมื่อเข้าสู่ระบบสำเร็จ การดู/เล่นและ Favorite ในคลังบนเครื่องไม่ต้องล็อกอิน การบันทึกเข้า Store, ดาว, คอมเมนต์ และรายงานต้องล็อกอิน
- `data/metadata/`: ดึง HTML แบบ native, จำกัด 1 MiB, timeout รวม 10 วินาที, redirect ไม่เกิน 5 ครั้ง ตรวจ DNS ทุกปลายทางและ literal IP ปฏิเสธ loopback/private/link-local/CGNAT/IPv6 local ปิด proxy สำหรับ preview ไม่ส่ง cookie/session ของ WebView ไปดึง metadata
- `data/model/`: Game, GameDraft, GameIdentity, RatingTotals, GameComment และผลลัพธ์ต่าง ๆ ส่วน `data/Game.kt` มี typealias ให้ UI เดิมใช้ต่อได้
- `data/remote/FirestoreGameRepository.kt`: แทน Fake ใน AppViewModel ใช้ query `status == approved` ร่วมกับ createdAt/ratingAvg และ tag indexes; สถานะและ error ส่งผ่าน StateFlow
- `GameIdentity` canonicalize scheme/host/default port/root slash/dot path และตัด fragment ก่อน SHA-256 64 hex เก็บ query และ path case เดิมเพื่อไม่รวมเกมที่มีความหมายต่างกัน ทุกเส้นทางรับ URL ผ่าน `UrlPolicy.normalize` ก่อน
- บันทึกด้วย create-if-absent transaction; ถ้าซ้ำส่ง `AlreadyExists` พร้อมปุ่มไปหน้าเกมเดิม ไม่มีการเขียนทับข้อมูลเจ้าของเดิม
- ดาว: อ่านคะแนนเดิมและ aggregates แล้วเขียนคะแนนและผลรวม/จำนวน/ค่าเฉลี่ยใน transaction เดียว เพิ่ม/แก้/ลบใช้ส่วนต่างและ createdAt เดิม หน้า Detail ฟังเกมและคะแนนของบัญชีปัจจุบันแบบ realtime และถอด listener เมื่อออกจากหน้า/เปลี่ยนบัญชี
- คอมเมนต์: สร้างพร้อม counter ใน batch เดียว; ลบใช้ transaction เพื่อกันการลบพร้อมกันทำให้ counter ลดซ้ำ หน้า Detail โหลดทีละ 20 ใหม่ก่อน ใช้ timestamp+document ID cursor เพื่อให้ paging ต่อได้แม้ boundary comment ถูกลบ และมีปุ่ม refresh/retry
- Reports เขียนเข้า Firestore จริงและมี snackbar ยืนยัน ไม่เปิดสิทธิ์อ่าน reports ให้ client
- คลังยังใช้ `RecentStore` เดิม ทั้ง guest และบัญชี: hydrate ชื่อ/ภาพ/คะแนนจาก Firestore และ fallback metadata ในเครื่อง ไม่แก้การทำงานของ WebView, GameBridge, UrlPolicy หรือ `MainActivity.play`

## Rules และความถูกต้องของ counters

`firestore.rules` จำกัดการแก้ metadata ให้เจ้าของ ไม่เปิดให้เจ้าของแก้ URL, ผู้ส่ง, status หรือยืนยันตัวเอง ผู้ใช้ทั่วไปแก้ aggregates ได้ต่อเมื่อ `getAfter`/`existsAfter` ของ rating หรือ comment ของตัวเองตรงกับส่วนต่างจริงเท่านั้น เอกสารลูกบังคับให้เขียน aggregate ของ parent พร้อมกันด้วย

เพิ่ม field ภายใน `commentMutationId` ในเอกสารเกมเพื่อให้ Rules ระบุ comment ที่เป็นสาเหตุของการเพิ่ม/ลด counter ได้ บันทึก field นี้ในชุด write เดียวกับ comment; client เปลี่ยน pointer เปล่า ๆ หรือ replay การเพิ่ม counter เดิมไม่ได้ ไม่มี comment edit และลบได้เฉพาะของตนเอง Reports สร้างได้เท่านั้นและอ่านไม่ได้

Rules ตรวจรูปแบบ game ID 64 hex แต่ Firestore Rules ไม่คำนวณ SHA-256 ของ URL ให้ จึงบังคับการ hash ที่ client และความ immutable ของ URL; client ที่แก้แอปเองยังอาจสร้าง URL เดียวกันด้วย ID hex อื่นได้ หากต้องบังคับ uniqueness กับ client ที่เป็นปฏิปักษ์อย่างสมบูรณ์ ให้ย้ายการสร้างไป trusted backend

## ทดสอบ

Core tests ใช้ Kotlin compiler/stdlib ที่ Gradle cache ไว้และ Jsoup/OkHttp จริง ไม่เพิ่ม Android test dependency:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/test-core.ps1
node --test tests/display.test.cjs
```

Rules tests ใช้ demo project บน Emulator เท่านั้น ไม่เชื่อม production:

```powershell
cd tests/firestore
npm install
npm test
```

มี test สำหรับ public/pending read, create validation, owner fields, เพิ่ม/แก้/ลบดาว, ข้าม UID, ปลอม aggregate/average, คอมเมนต์และ counter, replay write, profile และ reports ให้รันชุดนี้ก่อน deploy จริง ต้องมี Node 18+ และ Java พร้อมใช้งาน

`scripts/test-core.ps1` รัน `tests/AuthSessionTests.kt` เพิ่มด้วย เพื่อยืนยันว่าความล้มเหลว/timeout ของ Firestore ไม่ลบสถานะล็อกอิน, retry ได้โดยไม่ authenticate ซ้ำ, logout ยกเลิกงานเก่า และผลของบัญชีก่อนหน้าไม่เขียนทับสถานะบัญชีใหม่

ทดสอบเครื่องจริงด้วยบัญชี Google 2 บัญชี: เพิ่ม URL → แก้ preview → บันทึก → เพิ่ม URL ที่เทียบเท่าเดิมแล้วต้องพบ duplicate → ให้/แก้/ลบดาวจากอีกบัญชี → ส่งคอมเมนต์และโหลด 20+ รายการ → ลบเฉพาะของตน → report ของคนอื่น → logout ขณะเปิด Detail → ตรวจ listener/คะแนนของบัญชีใหม่ → ปิดเน็ตแล้วตรวจ cached Store และข้อความ retry → ตรวจ deep link เดิม

## ข้อจำกัดและขั้นถัดไป

- รูปเป็น hotlink เจ้าของเว็บลบ/เปลี่ยนหรือกัน hotlink ได้ รูปที่ redirect จะ fallback แทน เนื่องจาก image loader ปิด automatic redirect เพื่อไม่ให้ literal-IP redirect ข้าม DNS guard; พิจารณา trusted image proxy/Storage เมื่อมี backend ที่ตรวจรูปได้
- Native metadata อ่าน HTML ต้นทาง ไม่รัน JavaScript เว็บที่ใส่ OG หลังรัน JavaScript หรือต้อง login อาจได้ข้อมูลไม่ครบ ผู้ใช้กรอกเองและเล่นต่อได้ ข้อมูลเฉพาะใน WebView session ไม่ถูกส่งให้ fetcher
- Firestore persistence เป็นค่าเริ่มต้นของ Android อ่าน cache ได้เมื่อ offline แต่ transaction ต้องมีอินเทอร์เน็ต Batch ของ comment/report อาจรอ server acknowledgement แล้วค่อยแสดงสำเร็จเมื่อเชื่อมต่อ ห้ามถือว่า write ที่กำลังรอถูกยกเลิกเพียงเพราะผู้ใช้ปิดหน้า
- ชุด catalog query ดึงล่าสุด 100 และคะแนนสูง 100 แล้วรวมกัน; search เป็นการค้นหาในชุดที่โหลดและกรอง tag ด้วย query เพิ่ม ยังไม่ใช่ full-text search ของฐานข้อมูลทั้งหมด
- Fake 14 เกมคงไว้สำหรับ `@Preview` และการพัฒนาเท่านั้น runtime ใช้ Firestore จริง ไม่มีการ seed ตัวอย่างเข้า production การคัดสรร/Verified/AI tools/screenshots เพิ่มได้จาก trusted admin แต่ยังไม่มี moderation UI
- ตั้ง status เป็น approved ตอนสร้างตามสเปก ยังไม่มี abuse rate limit หรือระบบ moderator การให้คะแนนทั้งหมดลงเกมเอกสารเดียวอาจเกิด contention เมื่อ traffic สูง
- Cloud Functions (ต้องแผน Blaze) ช่วยย้าย validation, uniqueness, moderation และ aggregates ไป trusted server ลดความซับซ้อนของ Rules แต่เพิ่มค่าบริการ/การ deploy และต้องออกแบบ idempotency กับ eventual consistency งานนี้ยังใช้ atomic client writes พร้อม Rules ตามที่ขอและไม่ได้เปิด Blaze

เอกสาร: [Firebase Google Sign-In](https://firebase.google.com/docs/auth/android/google-signin), [Firestore atomic writes](https://firebase.google.com/docs/firestore/manage-data/transactions), [Rules conditions](https://firebase.google.com/docs/firestore/security/rules-conditions), [Credential Manager](https://developer.android.com/identity/sign-in/credential-manager-siwg)
