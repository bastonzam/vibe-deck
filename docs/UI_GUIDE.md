# UI และการตรวจรับ

## ไฟล์

- `ui/theme/Color.kt`, `Type.kt`, `Theme.kt`: สีพื้นม่วงเข้ม, lime accent, typography และ light theme; interface ใช้ MaterialTheme tokens
- `ui/components/GameComponents.kt`: GameCard, FeaturedBanner, GameArtwork, SectionHeader, TagChip, VerifiedBadge, RatingPill, EmptyState, ShimmerPlaceholder
- `ui/store/`: carousel ด้วย HorizontalPager, หมวด, มาใหม่/ยอดนิยม/เลือกโดยทีมงาน, pull-to-refresh
- `ui/detail/`: hero, sticky Play, screenshots, expandable description, favorite/share/report, realtime rating และ comments
- `ui/library/`: ล่าสุด/ที่ชอบจาก RecentStore, gradient/domain fallback, เล่นซ้ำ, ตั้งค่าการแสดงผลและบัญชี
- `ui/search/`: ประวัติค้นหาถาวร, ตัวกรองแนว/AI/แนวจอ, ผลลัพธ์จาก repository
- `ui/add/`: URL validation, โหลด metadata, editable preview, บันทึกจริงและแจ้ง duplicate
- `ui/HomeScreen.kt`: navigation-compose 3 แท็บและ detail/add routes, auth gate และ lifecycle refresh
- `ui/AppViewModel.kt`: สร้าง FirestoreGameRepository และ AuthRepository, StateFlow ของธีม/Full Screen/Safe Area/แนวจอ
- `data/GameRepository.kt`: interface และ Fake สำหรับ preview; `data/remote` เป็น backend จริง

มี ViewModel/StateFlow แยกต่อหน้า ไม่มี Firestore calls ใน Composable ทุกปุ่มเล่นผ่าน callback เดิมไป `MainActivity.play(url)` เพื่อบันทึก Recent และเปิด GameActivity ไม่ได้แก้ GameActivity/GameBridge/UrlPolicy ในการรีดีไซน์หรือต่อ Firebase

## Preview และการทดสอบหน้าจอ

1. เปิด `@Preview` ของ GameCard, FeaturedBanner และ Store/Detail/Search/Library/Add ใน Android Studio ตัวอย่างใช้ 14 เกมและ gradient ไม่ต้องมีอินเทอร์เน็ต Preview รองรับ parameter ให้สลับธีมด้วย `VibeDeckTheme(darkTheme = false)`
2. ทดสอบ font scale 1.0, 1.5, 2.0 บนหน้าจอแคบและ landscape: การ์ดตัดข้อความยาว, action ใน Detail wrap เป็น FlowRow, chips/ภาพเลื่อนแนวนอน, ฟอร์มและตั้งค่าเลื่อนแนวตั้ง, ดาวแต่ละดวงกดได้ 48 dp
3. Store: เลื่อน carousel/indicator, เปลี่ยนหมวด, แตะภาพไป Detail, กด Play แล้วกลับมา, กดดูทั้งหมด, pull-to-refresh ทั้งข้อมูลว่าง/โหลด/ผิดพลาด
4. Detail: sticky Play ไม่ทับเนื้อหา/gesture area, favorite เปลี่ยนในคลัง, share เปิด Android chooser, report ส่งจริงหลัง login, ลองคำอธิบายยาวและ URL ยาว
5. Library: สลับล่าสุด/ที่ชอบและกลับจากเกม, ลิงก์ไม่อยู่ใน catalog มีโดเมน/รูป fallback, ตั้งค่า Full Screen/Safe Area/แนวจอ/ธีมแล้วเปิดเกมใหม่, ตรวจค่าหลังปิดเปิดแอป
6. Search: query แบบไทย/อังกฤษ, บันทึกประวัติเมื่อกดค้นหาหรือเลือกผลลัพธ์, ล้างประวัติ/ตัวกรอง, no-result state, error/retry
7. Add: ลิงก์ไม่ถูกต้อง, metadata ครบ/ไม่ครบ/ล้มเหลว, แก้ข้อความก่อนบันทึก, ยกเลิก login แล้วข้อมูลฟอร์มยังอยู่, login แล้วบันทึกต่ออัตโนมัติ, เพิ่ม URL เดิมแล้วเปิดเกมเดิม, กดเล่นได้ก่อน login
8. Accessibility: เปิด TalkBack ตรวจคำอธิบายภาพ/ไอคอน, ลำดับ focus และข้อความ loading/error; ปุ่มอย่างน้อย 48 dp ยกเว้นองค์ประกอบที่ไม่ใช่ปุ่ม เช่น badge/rating

ภาพจริงใช้ Coil; ขณะโหลดเป็น pulse shimmer และเมื่อรูปเสียยังเห็น gradient + ตัวอักษรแรกของชื่อ การ์ดมี press scale เล็กน้อย, tab ใช้ AnimatedContent/Navigation transitions โดยไม่มีงานเครือข่ายบน main thread

## ไอคอน

ต้นฉบับ `app/src/main/assets/mock-icon/mock-neon.png` คงเดิม สคริปต์ `scripts/generate-icons.ps1` สร้าง:

| โฟลเดอร์ | ขนาด | ไฟล์ |
| --- | --- | --- |
| mipmap-mdpi | 48×48 | ic_launcher.png / ic_launcher_round.png |
| mipmap-hdpi | 72×72 | ic_launcher.png / ic_launcher_round.png |
| mipmap-xhdpi | 96×96 | ic_launcher.png / ic_launcher_round.png |
| mipmap-xxhdpi | 144×144 | ic_launcher.png / ic_launcher_round.png |
| mipmap-xxxhdpi | 192×192 | ic_launcher.png / ic_launcher_round.png |
| res/ | 512×512 | ic_launcher-web.png |

Square/Play Store เป็น PNG ทึบ, round มีขอบวงกลมโปร่งใสแบบ antialias ใช้ภาพเดิมและ resize แบบคงสัดส่วน ไม่สร้างโลโก้ใหม่ Manifest อ้าง `@mipmap/ic_launcher` และ `@mipmap/ic_launcher_round` แล้ว
