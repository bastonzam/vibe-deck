package app.vibedeck.data

typealias Game = app.vibedeck.data.model.Game

object SampleGames {
    val all = listOf(
        game(1, "วงโคจรสุดท้าย", "Arcade", "Claude", "Mali Studio", "ขับยานลำจิ๋วผ่านสนามดาวเคราะห์ เก็บแสงสุดท้ายแล้วกลับบ้านให้ได้", true),
        game(2, "สวนบนดวงจันทร์", "Idle", "Cursor", "Lunar Lab", "ปลูกสวนเล็ก ๆ บนดวงจันทร์ ให้เวลาและความสงบช่วยเติมสีสันให้จักรวาล", true),
        game(3, "แมวกับกล่องปริศนา", "Puzzle", "ChatGPT", "Neko Works", "กล่องหนึ่งใบ แมวหนึ่งตัว และปริศนาที่เปลี่ยนทุกครั้งที่คุณขยับ", true),
        game(4, "เมืองหลังเที่ยงคืน", "Strategy", "Claude", "Night Owl", "สร้างเมืองที่ไม่เคยหลับ วางแผนพลังงานและดูแลชุมชนใต้แสงดาว", false),
        game(5, "ก้าวข้ามเมฆ", "Platformer", "Cursor", "Cloud Club", "กระโดดตามจังหวะลม สำรวจเกาะลอยฟ้า และพบเส้นทางที่คุณเลือกเอง", true),
        game(6, "จังหวะสายฝน", "Arcade", "Gemini", "Rain Room", "แตะตามเสียงฝนและสร้างท่วงทำนองของคุณในคืนที่เมืองเงียบลง", false),
        game(7, "ร้านชาของภูตน้อย", "Idle", "Claude", "Tiny Tea", "ชงชาให้ภูตในป่า เก็บสูตรใหม่ และค่อย ๆ สร้างร้านที่อบอุ่นที่สุด", true),
        game(8, "ประตูมิติสีคราม", "Puzzle", "Cursor", "Blue Door", "หมุนห้อง เชื่อมเส้นทาง และไขความลับของประตูที่ไม่มีวันเหมือนเดิม", false),
        game(9, "กองเรือกระดาษ", "Strategy", "ChatGPT", "Paper People", "พากองเรือกระดาษฝ่ากระแสน้ำด้วยการวางแผนที่เรียบง่ายแต่ท้าทาย", true),
        game(10, "วิ่งส่งดาว", "Platformer", "Gemini", "Star Post", "เป็นบุรุษไปรษณีย์แห่งทางช้างเผือก ส่งดาวดวงเล็กให้ถึงที่หมาย", false),
        game(11, "ฟองสบู่วันหยุด", "Arcade", "Claude", "Soft Pixel", "ต่อฟองสบู่สีพาสเทล ทำคอมโบ และพักใจสั้น ๆ ระหว่างวัน", true),
        game(12, "หอคอยความคิด", "Puzzle", "Cursor", "Idea Stack", "เรียงความคิดให้กลายเป็นหอคอย ฝึกมองมุมใหม่ในแต่ละด่าน", false),
        game(13, "สถานีปลายสวน", "Idle", "ChatGPT", "Green Station", "ดูแลสถานีรถไฟเล็กในสวน ต้อนรับผู้เดินทางและตกแต่งพื้นที่ของคุณ", true),
        game(14, "อาณาจักรจิ๋ว", "Strategy", "Claude", "Little Kingdom", "สร้างอาณาจักรบนโต๊ะทำงาน ทุกการตัดสินใจเล็ก ๆ เปลี่ยนโลกใบนี้", true),
    )

    private fun game(id: Int, title: String, tag: String, tool: String, developer: String, description: String, verified: Boolean) = Game(
        id = "game-$id", title = title, url = "https://example.com/games/$id",
        description = description, tags = listOf(tag), aiTools = listOf(tool),
        orientation = if (tag in listOf("Idle", "Puzzle")) "portrait" else "landscape",
        usesAudio = id % 3 == 0, developerName = developer, isVerified = verified,
        rating = 4.2 + (id % 8) / 10.0,
        screenshots = listOf("", "", ""),
        createdAtMillis = id * 1000L,
    )
}

data class LibraryState(val recent: List<Game> = emptyList(), val favorites: List<Game> = emptyList())
