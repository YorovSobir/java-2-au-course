# Simple FTP

Требуется реализовать сервер, обрабатывающий два запроса.

* list — листинг файлов в директории на сервере
* get — скачивание файла с сервера

И клиент, позволяющий исполнять указанные запросы.

Sockets:

* Google -> Java Sockets
* [https://docs.oracle.com/javase/tutorial/networking/sockets/clientServer.html](https://docs.oracle.com/javase/tutorial/networking/sockets/clientServer.html)

---

# List

Формат запроса:

    <1: Int> <path: String>
    path — путь к директории
Формат ответа:

    <size: Int> (<name: String> <is_dir: Boolean>)*,
    size — количество файлов и папок в директории
    name — название файла или папки
    is_dir — флаг, принимающий значение True для директорий
Если директории не существует, сервер посылает ответ с size = 0

---

# Get

Формат запроса:

    <2: Int> <path: String>
    path — путь к файлу
Формат ответа:

    <size: Long> <content: Bytes>,
    size — размер файла,
    content — его содержимое
Если файла не существует, сервер посылает ответ с size = 0

---

# Примечания:

* Разрешается использовать библиотеки для упрощения ввода-вывода
* Рекомендуется взглянуть на DataInputStream и DataOutputStream
* Должны быть реализованы тесты
* Рекомендуется задуматься об интерфейсе сервера и клиента, возможно стоит сделать что-то подобное:
    * Server: start/stop
    * Client: connect/disconnect/executeList/executeGet

Срок: <b>15.11.2017 23:59</b> 

---

# Формат сдачи

* Каждое задание выполняете в отдельной ветке в репозитории на GitHub
* Создаете pull request ветки в master этого же репозитория
* Тема PR: Java02. ДЗ 03, &lt;фамилия&gt; &lt;имя&gt;
* В комментарии упоминаете username преподавателя (@sproshev, @dsavvinov)
* Посылаете письмо преподавателю с такой же темой с ссылкой на pull request

Проверяйте табличку на странице курса на предмет того, что преподаватель получил увидел ваш PR
