# Электронное расписание

Генерация индивидуального расписания преподавателя в т.ч. в формате ICal для импорта на мобильные устройства и сервисы вроде Google Calendar.
Поиск информации о занятиях по дню недели, группе, преподавателю, аудитории.
Автоматическое скачивание и парсинг учебного расписания по указанным URL
Планировщик обновления.

В качестве основы используется онлайн скачивание и парсинг файлов расписания в формате Excel.
Структура XLS файла специфическая но является генерируемой распространенным ПО составления расписания в ВУЗах РФ.

В качестве отправной точки были взяты исходные тексты дипломного проекта студентки ИМЭИ ИГУ Вафиной Ренаты Рашидовны.

# Сборка и запуск

Проект реализован с помощью
PlayFramework на языках Java/Scala

для запуска с помощью SBT:
```bash
$ sbt run
```

для запуска с помощью Activator
```bash
$ activator run
```

Подробнее см. документацию по PlayFramework на сайте https://www.playframework.com/

#WAR-контейнер

Для размещения в с помощью традиционных Servlet-контейнеров добавлена поддержка генерации WAR файлов Servlet 3.1

для генерации с помощью SBT:
```bash
$ sbt war
```

для генерации с помощью Activator
```bash
$ activator war
```

Сгенерированный файл будет расположен в папке target.