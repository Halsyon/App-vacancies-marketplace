package app.vacancies.grabber.utils;

import org.quartz.*;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 */
public class Grabber implements Grab {

    private final Properties cfg = new Properties();

    public Store store() throws SQLException {
        PsqlStore psqlStore = new PsqlStore(cfg);
        return psqlStore;
    }

    public Scheduler scheduler() throws SchedulerException {
        Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
        scheduler.start();
        return scheduler;
    }

    public void cfg() throws IOException {
        try (InputStream in = new FileInputStream(new File("app.properties"))) {
            cfg.load(in);
        }
    }

    @Override
    public void init(Parse parse, Store store, Scheduler scheduler) throws SchedulerException {
        JobDataMap data = new JobDataMap();
        data.put("store", store);
        data.put("parse", parse);
        data.put("hibernate.connection.link", cfg.getProperty("hibernate.connection.link"));
// В объект Scheduler мы будем добавлять задачи, которые хотим выполнять периодически.
// Вам нужно создать класс реализующий этот интерфейс.
//Внутри этого класса нужно описать требуемые действия.
        JobDetail job = newJob(GrabJob.class)
                .usingJobData(data)
                .build();
        SimpleScheduleBuilder times = simpleSchedule() // создание расписания
                .withIntervalInSeconds(Integer.parseInt(cfg.getProperty("rabbit.interval"))) // ранее ключ time
                .repeatForever();
        Trigger trigger = newTrigger()//задача выполняется через триггер
                .startNow() // начинаем сразу
                .withSchedule(times)
                .build();
        scheduler.scheduleJob(job, trigger);
    }

    public static class GrabJob implements Job {
// вариант №1
//PsqlStore реализует Store и все его методы (save(Post post), List<Post> getAll(), Post findById(int id))
//SqlRuParse методы интерфеса Parse(List<Post> list(String link), Post detail(String link))
//Grabber implements Grab и все его методы (void init(Parse parse, Store store, Scheduler scheduler)))
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            JobDataMap map = context.getJobDetail().getJobDataMap();
            Store store = (Store) map.get("store"); //метод save(Post) interface Store / -PsqlStore и его методы
            Parse parse = (Parse) map.get("parse"); // SqlRuParse and this methods
            String stringToParse = map.getString("hibernate.connection.link");
            List<Post> postArrayList = new ArrayList<>(); //для ссыллок последующих включая титульную
            List<String> stringList = new ArrayList<>();
            Post post = new Post();
            List<Post> listAfterBD = new ArrayList<>();

            try {
                postArrayList = parse.list(stringToParse);
                for (int i = 2; i < 6; i++) {
                    //записали в Лист все топики в сущностти Post(53 шт)
                    postArrayList = parse.list(stringToParse + "/" + i);
                    //saving in database
                    for (Post post1 : postArrayList) {
                        try {
                            store.save(post1);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                try {
                    var listAfterDB = store.getAll();
                    System.out.println("listAfterDB size :" + listAfterDB.size());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                post = store.findById(3);
                System.out.println("what find by index : " + post.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

/*   //вариант №2 с вычислением все ссылок на последующие старницы
        //начиная с титольной(входящей) и последующих 4 страницы
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            JobDataMap map = context.getJobDetail().getJobDataMap();
            Store store = (Store) map.get("store"); //метод save(Post) interface Store / -PsqlStore и его методы
            Parse parse = (Parse) map.get("parse"); // SqlRuParse and this methods
            String stringToParse = map.getString("hibernate.connection.link");
            List<Post> postArrayList = new ArrayList<>(); //для ссыллок последующих включая титульную
            List<String> stringList = new ArrayList<>();
            Post post = new Post();
            List<Post> listAfterBD = new ArrayList<>();
            // post = parse.list(stringToParse); // записали в Лист все топики в сущностти Post(53 шт)
            try {
                stringList.add(stringToParse);
                Document document = Jsoup.connect(stringToParse).get();
                Elements elements = document.select(".sort_options");
                Elements link = elements.select("a[href]");
                for (Element element : link) {
                    stringList.add(element.attr("href")); // добавили все ссылки на след старницы
                }
                for (int i = 0; i < 5; i++) {
                    postArrayList = parse.list(stringList.get(i)); // топики с текущей стриницы/ссылки в лист
                    // postArrayList = parse.list(stringToParse); // записали в Лист все топики в сущностти Post(53 шт)
                    System.out.println("размер листа после парсинга страницы : " + postArrayList.size());
                    System.out.println("выборочно сущность из этого списака : индекс 3 - " + postArrayList.get(3));
                    //saving in database
                    for (Post post1 : postArrayList) {
                        try {
                            store.save(post1);
                            System.out.println("Everything is ok");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                try {
                    var listAfterDB = store.getAll();
                    System.out.println("listAfterDB size :" + listAfterDB.size());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                post = store.findById(3);
                System.out.println("what find by index : " + post.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
            //PsqlStore psqlStore = store.
            //PsqlStore реализует Store и все его методы (save(Post post), List<Post> getAll(), Post findById(int id))
            //SqlRuParse методы интерфеса Parse(List<Post> list(String link), Post detail(String link))
            //Grabber implements Grab и все его методы (void init(Parse parse, Store store, Scheduler scheduler)))
            System.out.println("Закончили всек манипуляции");
        }*/
    }

    //В разделе IO мы делали сервер EchoServer. В этом задании сделаем тоже самое
    // только ответ от сервера будет в виде списка вакансий.
    public void web(Store store) {
        new Thread(() -> {
            try (ServerSocket server = new ServerSocket(Integer.parseInt(cfg.getProperty("port")))) {
                while (!server.isClosed()) {
                    Socket socket = server.accept();
                    try (OutputStream out = socket.getOutputStream()) {
                        out.write("HTTP/1.1 200 OK\r\n\r\n".getBytes());
                        for (Post post : store.getAll()) {
                            out.write(
                                    //Примечание. При возникновении проблем с кодировкой на Windows,
                                    // нужно указать кодировку при выводе Windows-1251
                                    //post.toString().getBytes(Charset.forName("Windows-1251"))
                                    post.toString().getBytes(Charset.forName("Windows-1251")));
                            out.write(System.lineSeparator().getBytes());
                        }
                    } catch (IOException io) {
                        io.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static void main(String[] args) throws Exception {
        //для передачи в конструктор класса SqlRuParse интерфейса, передаем класс его реализующий  SqlDateTimeParser
        SqlDateTimeParser sqlDateTimeParser = new SqlDateTimeParser();
        Grabber grab = new Grabber();
        grab.cfg(); // прогрузили ключи , установили соединение
        Scheduler scheduler = grab.scheduler(); // запустили планировщик
        Store store = grab.store(); // подгрузили PsqlStore чтобы добраться до переопределенных методов интерфейса Store
        grab.init(new SqlRuParse(sqlDateTimeParser), store, scheduler);
        grab.web(store); //эхо сервер - вывод данных на страницу по URL http://localhost:9000
        // должны увидеть строковое представление найденных вами вакансий на сранице
    }
}
