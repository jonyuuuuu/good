package org.example;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Services {
    public static final String SPREADSHEET_ID = "1YXo9JeQ-stiU_dlAt10VGIWtMy4EwaJ6XyHUDeDDFEE";
    public static final String RANGE = "A1:ZZZ10000";
    public static String groupName = "24/КС-191к";

    public static Sheets createSheetsService() {
        try {
            InputStream credentialsStream = Services.class.getClassLoader().getResourceAsStream("credentials.json");

            if (credentialsStream == null) {
                throw new IOException("Файл credentials.json не найден в resources");
            }

            GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream)
                    .createScoped(Arrays.asList("https://www.googleapis.com/auth/spreadsheets.readonly"));

            return new Sheets.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(), new HttpCredentialsAdapter(credentials))
                    .setApplicationName("Your Application Name")
                    .build();
        } catch (IOException | GeneralSecurityException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<List<Object>> getSchedule(Sheets sheetsService, String spreadsheetId, String range) throws IOException {
        ValueRange result = sheetsService.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute();
        return result.getValues();
    }

    public static String printGroupSchedule(List<List<Object>> schedule, String groupName) {
        if (schedule.size() < 3) {
            return "Ошибка.";
        }

        List<Object> headers = schedule.get(2);
        int groupIndex = -1;

        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i).toString().replace(" ", "");
            if (header.equals(groupName)) {
                groupIndex = i;
                break;
            }
        }

        if (groupIndex == -1) {
            return "Группа " + groupName + " не найдена.";
        }

        StringBuilder result = new StringBuilder("Расписание для группы " + groupName + ":\n");
        int[][] dayRanges = {
                {5, 16},
                {17, 28},
                {29, 40},
                {41, 52},
                {53, 64},
                {65, 72}
        };

        int[][] pairNumbers = {
                {1, 2, 3, 4, 5, 6},
                {1, 2, 3, 4, 5, 6},
                {1, 2, 3, 4, 5, 6},
                {1, 2, 3, 4, 5, 6},
                {1, 2, 3, 4, 5, 6},
                {1, 2, 3, 4, 5, 6}
        };

        String[] daysOfWeek = {"Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота"};

        for (int day = 0; day < dayRanges.length; day++) {
            result.append("\n" + daysOfWeek[day] + ":" + "\n" + "\n");
            for (int i = dayRanges[day][0]; i <= dayRanges[day][1]; i++) {
                List<Object> row = schedule.get(i);
                if (!(i % 2 == 0)) {
                    if (groupIndex < row.size()) {
                        Object lesson = row.get(groupIndex);
                        if (lesson != null && !lesson.toString().isEmpty() && !lesson.toString().equals(" ")) {
                            Object room = (groupIndex + 1 < row.size()) ? row.get(groupIndex + 1) : "Кабинет не найден";
                            room = cleanString2(cleanString(room.toString()));

                            String teacher = "";
                            if (i + 1 < schedule.size()) {
                                List<Object> nextRow = schedule.get(i + 1);
                                if (groupIndex < nextRow.size()) {
                                    teacher = ", Препод " + nextRow.get(groupIndex);
                                    teacher = cleanString2(cleanString(teacher));
                                }
                            }

                            String cleanedLesson = cleanString2(cleanString(lesson.toString()));

                            int pairIndex = (i - dayRanges[day][0]) / 2;
                            int pairNumber = pairNumbers[day][pairIndex];

                            result.append("  " + pairNumber + ": " + cleanedLesson + ", Кабинет " + room + (teacher.isEmpty() ? "" : teacher) + "\n");
                        }
                    }
                }
            }
        }

        return result.toString();
    }
    public static String printTeacherSchedule(List<List<Object>> schedule, String teacherName) {
        if (schedule.size() < 3) {
            return "Ошибка.";
        }

        StringBuilder result = new StringBuilder("Расписание для преподавателя " + teacherName + ":\n");

        String[] daysOfWeek = {"Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота"};

        int[][] dayRanges = {
                {5, 16},
                {17, 28},
                {29, 40},
                {41, 52},
                {53, 64},
                {65, 72}
        };

        for (int day = 0; day < dayRanges.length; day++) {
            boolean hasClasses = false;
            result.append("\n" + daysOfWeek[day] + ":\n");

            for (int i = dayRanges[day][0]; i <= dayRanges[day][1]; i++) {
                List<Object> row = schedule.get(i);

                if (i % 2 == 0) {
                    for (int col = 2; col < row.size(); col += 2) {
                        String lesson = "";
                        String room = "";
                        String group = "";

                        if (row.get(col).toString().contains(teacherName)) {
                            lesson = schedule.get(i - 1).get(col).toString();

                            int roomCol = col + 1;
                            int roomRow = i - 1;
                            room = schedule.get(roomRow).get(roomCol).toString();

                            group = schedule.get(2).get(col).toString();

                            String pairNumber = schedule.get(i - 1).get(1).toString();

                            result.append("Преподаватель - " + row.get(col) + "  Группа " + group + " - Пара " + pairNumber + ": " + lesson + ", Кабинет " + room + "\n");

                            hasClasses = true;
                        }
                    }
                }
            }

            if (!hasClasses) {
                result.append("  Преподаватель не проводит пары в этот день.\n");
            }
        }

        return result.toString();
    }

    public static List<String> getAllGroups(List<List<Object>> schedule) {
        if (schedule.size() < 3) {
            return null;
        }

        List<Object> headers = schedule.get(2);
        List<String> groups = new ArrayList<>();

        for (int i = 0; i < headers.size(); i++) {
            if(headers.get(i) != null && !headers.get(i).toString().equalsIgnoreCase("") && !headers.get(i).toString().equalsIgnoreCase(" ")) {
                String header = headers.get(i).toString().replace(" ", "");
                groups.add(header);
            }
        }
        return groups;
    }
    static String cleanString(String str) {
        return str.replace("/  ", "");
    }

    static String cleanString2(String str) {
        return str.replace("/ ", "").replace("\n", "");
    }
}
