package com.mailserver.mailprocessor;

import com.mailserver.mailprocessor.model.dto.RequestDto;
import com.mailserver.mailprocessor.model.enums.RequestCategory;
import com.mailserver.mailprocessor.service.EmailParserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class EmailParserServiceTest {

    @Autowired
    private EmailParserService emailParserService;

    @Test
    void testParseEmailWithAllFields() {
        String subject = "Заявка на техподдержку";
        String body = """
            Организация: ООО Тестовая компания
            ФИО: Иванов Иван Иванович
            Телефон: +7 999 123-45-67
            Email: ivanov@test.ru
            Тип прибора: Счетчик электроэнергии
            Серийный номер: ABC123456
            ИНН: 7707123456
            Страна: Россия
            Проект: Проект А
            
            Описание проблемы:
            Прибор не включается
            """;
        String from = "ivanov@test.ru";

        RequestDto result = emailParserService.parseEmailToRequest(subject, body, from);

        assertNotNull(result);
        assertEquals("ivanov@test.ru", result.getEmail());
        assertTrue(result.getOrganization().contains("Тестовая компания"));
        assertTrue(result.getFio().contains("Иванов"));
        assertNotNull(result.getPhone());
        assertEquals(RequestCategory.TECHNICAL_SUPPORT, result.getCategory());
    }

    @Test
    void testDetermineCategory() {
        String subject = "Вопрос по гарантии";
        String body = "У меня вопрос по гарантии на прибор";
        String from = "test@test.ru";

        RequestDto result = emailParserService.parseEmailToRequest(subject, body, from);

        assertEquals(RequestCategory.WARRANTY, result.getCategory());
    }

    @Test
    void testExtractPhoneNumber() {
        String subject = "Заявка";
        String body = "Телефон: +7 (999) 123-45-67";
        String from = "test@test.ru";

        RequestDto result = emailParserService.parseEmailToRequest(subject, body, from);

        assertNotNull(result.getPhone());
    }
}
