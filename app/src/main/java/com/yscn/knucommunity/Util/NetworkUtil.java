package com.yscn.knucommunity.Util;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;

import com.yscn.knucommunity.Items.CommentListItems;
import com.yscn.knucommunity.Items.DefaultBoardListItems;
import com.yscn.knucommunity.Items.DeliveryListItems;
import com.yscn.knucommunity.Items.LibrarySeatItems;
import com.yscn.knucommunity.Items.MajorDetailItems;
import com.yscn.knucommunity.Items.MajorSimpleListItems;
import com.yscn.knucommunity.Items.MeetingListItems;
import com.yscn.knucommunity.Items.NoticeItems;
import com.yscn.knucommunity.Items.SchoolRestrauntItems;
import com.yscn.knucommunity.Items.ShareTaxiListItems;
import com.yscn.knucommunity.R;
import com.yscn.knucommunity.Ui.AlertToast;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.params.CoreProtocolPNames;
import org.json.JSONException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Created by GwonHyeok on 14. 11. 19..
 */
public class NetworkUtil {
    private static NetworkUtil instance;
    private HttpClient httpClient = new DefaultHttpClient();

    private NetworkUtil() {

    }

    public synchronized static NetworkUtil getInstance() {
        if (instance == null) {
            instance = new NetworkUtil();
        }
        String userAgent = System.getProperty("http.agent");
        instance.httpClient.getParams().setParameter(CoreProtocolPNames.USER_AGENT, userAgent);
        instance.syncCookie();
        return instance;
    }

    private static int parseInt(String str) {
        return Integer.parseInt(str.replace(" ", ""));
    }

    public NetworkUtil checkIsLoginUser() {
        try {
            if (!isLoginUser()) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        AlertToast.error(ApplicationContextProvider.getContext(), R.string.error_empty_studentnumber_info);
                        UserData.getInstance().logoutUser();
                    }
                });
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return instance;
    }

    public boolean isLoginUser() throws IOException, ParseException {
        log("isLoginUser Check Logic");
        JSONParser jsonParser = new JSONParser();
        HttpResponse httpResponse = getData(UrlList.LOGIN_CHECK_URL);
        JSONObject jsonObject = (JSONObject) jsonParser.parse(new InputStreamReader(httpResponse.getEntity().getContent()));
        String result = jsonObject.get("result").toString();
        return !result.equals("fail");
    }

    public LoginStatus RegisterAppServer(String studentnumber, String password, String nickname, String name, Uri profileURI) throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        ContentType contentType = ContentType.create("text/plain", Charset.forName("UTF-8"));

        HttpPost httpPost = new HttpPost(UrlList.APP_REGISTER_URL);
        HttpEntity entity = MultipartEntityBuilder.create()
                .addTextBody("studentnumber", studentnumber, contentType)
                .addTextBody("password", password, contentType)
                .addTextBody("nickname", nickname, contentType)
                .addTextBody("name", name, contentType)
                .addBinaryBody("userfile", new File(ApplicationUtil.getInstance().UriToPath(profileURI)))
                .build();
        httpPost.setEntity(entity);
        HttpResponse httpResponse = httpClient.execute(httpPost);

        InputStreamReader inputStreamReader =
                new InputStreamReader(httpResponse.getEntity().getContent());
        JSONObject resultObject = (JSONObject) jsonParser.parse(inputStreamReader);
        if (checkResultData(resultObject)) {
            String status = resultObject.get("status").toString();
            if (status.equals("hasmember")) {
                /* 이미 회원이 있으나 회원가입을 요청 했을때 */
                return LoginStatus.HASMEMBER;
            } else if (status.equals("bannickname")) {
                /* 사용할 수 없는 닉네임일때 */
                return LoginStatus.BANNICKNAME;
            } else if (status.equals("registersuccess")) {
                /* 회원 가입 성공 */
                return LoginStatus.SUCCESS;
            }
        } else {
            /* 서버의 문제가 있을때 */
            return LoginStatus.FAIL;
        }
        /* 그외의 예상 하지 못한 오류때문에 */
        return LoginStatus.FAIL;
    }

    public boolean editProfilePicture(Uri uri) throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();

        File profileImageFile = new File(ApplicationUtil.getInstance().UriToPath(uri));

        HttpPost httpPost = new HttpPost(UrlList.PROFILE_IMAGE_EDIT_URL);
        HttpEntity entity = MultipartEntityBuilder.create()
                .addBinaryBody("userfile", profileImageFile)
                .build();
        httpPost.setEntity(entity);
        HttpResponse httpResponse = httpClient.execute(httpPost);

        JSONObject jsonObject = (JSONObject) jsonParser.parse(
                new InputStreamReader(httpResponse.getEntity().getContent())
        );

        if (checkResultData(jsonObject)) {
            return jsonObject.get("status").equals("success");
        }
        return false;
    }


    public JSONObject deleteProfilePicture() throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        HttpResponse httpResponse = getData(UrlList.PROFILE_IMAGE_DELETE_URL);
        return (JSONObject) jsonParser.parse(new InputStreamReader(httpResponse.getEntity().getContent()));
    }

    public LoginStatus LoginAppServer(String studentnumber, String password) throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        HashMap<String, String> parameters = new HashMap<>();
        String studentname = "";
        parameters.put("user_id", studentnumber);
        parameters.put("user_pwd", password);
        HttpResponse httpResponse = postData(UrlList.SCHOLL_SERVER_LOGIN_URL, parameters);
        JSONObject loginjsonObject = (JSONObject) jsonParser.parse(
                new InputStreamReader(httpResponse.getEntity().getContent()));
        String result = loginjsonObject.get("result").toString();

        /*
         * 학교 서버 로그인에 성공함
         * 앱서버 로그인 확인
         */
        if (result.equals("success")) {
            Header[] headers = httpResponse.getHeaders("Set-Cookie");
            for (Header header : headers) {
                if (header.getValue().split("=")[0].equals("mast_name_e")) {
                    String name = header.getValue().split(";")[0];
                    name = name.replace("mast_name_e=", "").replace("\"", "");
                    byte[] bytes = Base64.decode(name, 0);
                    studentname = URLDecoder.decode(new String(bytes, "UTF-8"), "UTF-8");
                }
            }

            /* 앱서버 post 파라미터 생성 */
            HashMap<String, String> appLoginParameters = getTextParameters(
                    new String[]{"studentnumber", "password"}, new String[]{studentnumber, password});
            httpResponse = postData(UrlList.APP_LOGIN_URL, appLoginParameters);
            JSONObject apploginjsonObject = (JSONObject) jsonParser.parse(
                    new InputStreamReader(httpResponse.getEntity().getContent()));
            /* 서버 Response 확인 */
            if (checkResultData(apploginjsonObject)) {
                /* 로그인 작업시 나온 이름, 학번정보를 메모리 상에 올려놓음 */
                UserData.getInstance().setStudentNumber(studentnumber);
                UserData.getInstance().setStudentName(studentname);
                log(apploginjsonObject.toJSONString());
                log("Student Name : " + studentname);
                result = apploginjsonObject.get("status").toString();
                /*
                 * 앱 서버에 멤버가 있을 경우 Token 발급
                 * 없는 경우에 Nomember 리턴
                 */
                if (result.equals("hasmember")) {
                    /* 멤버가 있을경우 토큰을 메모리에 올려놓고 Preference에 저장 */
                    String token = apploginjsonObject.get("token").toString();
                    String rating = apploginjsonObject.get("rating").toString();
                    UserData.getInstance().setUserToken(token);
                    log("Token  : " + token);

                    log("Save User Data");
                    UserDataPreference preference = new UserDataPreference(ApplicationContextProvider.getContext());
                    preference.removeAll();
                    preference.setStudentNumber(studentnumber);
                    preference.setToken(token);
                    preference.setStudentName(studentname);
                    preference.setStudentRating(rating);

                    /* HttpClient 에서 쿠키를 가져와 Seesion 값 저장 */
                    syncCookie();
                    return LoginStatus.SUCCESS;
                } else if (result.equals("nomember")) {
                    return LoginStatus.NOMEMBER;
                } else {
                    /* 예상 하지 못한 다른 경우에서 로그인 실패 */
                    return LoginStatus.FAIL;
                }
            } else {
                /* 앱 서버 로그인시 Respone 에 문제가 있을 경우 */
                return LoginStatus.FAIL;
            }
        } else {
            /* 학교 서버 로그인 실페 */
            return LoginStatus.FAIL;
        }
    }

    private void syncCookie() {
        Context context = ApplicationContextProvider.getContext();
        CookieStore cookieStore = ((DefaultHttpClient) httpClient).getCookieStore();
        List<Cookie> cookieList = cookieStore.getCookies();

        SessionDataPreference sessionDataPreference = new SessionDataPreference(context);

        String ci_session, path, domain;
        ci_session = sessionDataPreference.getSession();
        path = sessionDataPreference.getPath();
        domain = sessionDataPreference.getDomain();

        /* 만약 데이터에 저장되어있는 Seesion 값이 null 이면 현재 HttpClient 에 있는 Seesion값 가져와서 Prefernce 에 저장 */
        if (ci_session == null) {
            for (Cookie cookie : cookieList) {
                if (cookie.getName().equals("ci_session")) {
                    sessionDataPreference.putSession(ci_session = cookie.getValue());
                    sessionDataPreference.putDomain(domain = cookie.getDomain());
                    sessionDataPreference.putPath(path = cookie.getPath());
                }
            }
        }

        cookieStore.clear();
        BasicClientCookie basicClientCookie = new BasicClientCookie("ci_session", ci_session);
        basicClientCookie.setDomain(domain);
        basicClientCookie.setPath(path);
        cookieStore.addCookie(basicClientCookie);
    }

    /**
     * @return data[0] == REGISTERID, data[1] == APPVERSION
     * @throws IOException
     * @throws ParseException
     */
    public String[] getGCMRegisterData() throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        HttpResponse httpResponse = postData(UrlList.APP_GET_GCM_REGISTERID, null);
        JSONObject jsonObject = (JSONObject) jsonParser.parse(
                new InputStreamReader(httpResponse.getEntity().getContent())
        );
        String[] data = new String[2];
        if (checkResultData(jsonObject)) {
            data[0] = jsonObject.get("regid").toString();
            data[1] = jsonObject.get("appver").toString();
        }
        return data;
    }

    public JSONObject getAuthorDevicesList() throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        HttpResponse httpResponse = postData(UrlList.GET_AUTHOR_DEVICES_URL, null);
        return (JSONObject) jsonParser.parse(new InputStreamReader(httpResponse.getEntity().getContent()));
    }

    public JSONObject serverStatusCheck() throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        HttpResponse httpResponse = postData(UrlList.SERVER_STATUS_CHECK_URL, null);
        return (JSONObject) jsonParser.parse(new InputStreamReader(httpResponse.getEntity().getContent()));
    }

    public JSONObject doLogout() throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        HttpResponse httpResponse = postData(UrlList.LOGOUT_URL, null);
        return (JSONObject) jsonParser.parse(new InputStreamReader(httpResponse.getEntity().getContent()));
    }

    public JSONObject doLogoutWithSession(String session_id) throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        HashMap<String, String> parameter = new HashMap<>();
        parameter.put("session", session_id);
        HttpResponse httpResponse = postData(UrlList.LOGOUT_WITH_SESSION, parameter);
        return (JSONObject) jsonParser.parse(new InputStreamReader(httpResponse.getEntity().getContent()));
    }

    public boolean registerGCMID(int appVersion, String gcmID) throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        HashMap<String, String> parameter = new HashMap<>();
        parameter.put("appversion", String.valueOf(appVersion));
        parameter.put("gcmregisterid", gcmID);
        HttpResponse httpResponse = postData(UrlList.APP_REGISTER_GCM_REGISTERID, parameter);
        JSONObject jsonObject = (JSONObject) jsonParser.parse(
                new InputStreamReader(httpResponse.getEntity().getContent())
        );
        return checkResultData(jsonObject);
    }

    public JSONObject changeNickName(String nickname) throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        HashMap<String, String> parameter = new HashMap<>();
        parameter.put("nickname", nickname);
        HttpResponse httpResponse = postData(UrlList.CHANGE_NICKNAME_URL, parameter);
        return (JSONObject) jsonParser.parse(new InputStreamReader(httpResponse.getEntity().getContent()));
    }

    public JSONObject getSimpleProfile() throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        HttpResponse httpResponse = postData(UrlList.GET_SIMPLE_PROFILE_URL, null);
        return (JSONObject) jsonParser.parse(new InputStreamReader(httpResponse.getEntity().getContent()));
    }

    public JSONObject getMyNotify(int page) throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        HashMap<String, String> paramter = new HashMap<>();
        paramter.put("page", String.valueOf(page));
        HttpResponse httpResponse = postData(UrlList.MYINFO_MYNOTIFY_URL, paramter);
        return (JSONObject) jsonParser.parse(new InputStreamReader(httpResponse.getEntity().getContent()));
    }

    public JSONObject getMyBoardList(int page) throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        HashMap<String, String> paramter = new HashMap<>();
        paramter.put("page", String.valueOf(page));
        HttpResponse httpResponse = postData(UrlList.MYINFO_MYBOARD_URL, paramter);
        return (JSONObject) jsonParser.parse(new InputStreamReader(httpResponse.getEntity().getContent()));
    }

    public JSONObject getMyCommentList(int page) throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        HashMap<String, String> paramter = new HashMap<>();
        paramter.put("page", String.valueOf(page));
        HttpResponse httpResponse = postData(UrlList.MYINFO_MYCOMMENT_URL, paramter);
        return (JSONObject) jsonParser.parse(new InputStreamReader(httpResponse.getEntity().getContent()));
    }

    public JSONObject getMeetingData(String contentid) throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        HttpResponse httpResponse = postData(UrlList.GET_MEETING_CONTENT_URL + contentid, null);
        return (JSONObject) jsonParser.parse(new InputStreamReader(httpResponse.getEntity().getContent()));
    }

    public JSONObject updateMeetingResult(String meetingContentid, int result) throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        HashMap<String, String> parameter = new HashMap<>();
        parameter.put("matchingresult", String.valueOf(result));
        HttpResponse httpResponse = postData(UrlList.UPDATE_MEETING_RESULT + meetingContentid, parameter);
        return (JSONObject) jsonParser.parse(new InputStreamReader(httpResponse.getEntity().getContent()));
    }

    public ArrayList<MajorDetailItems> getMajorDetailInfo(int majorType) throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        ArrayList<MajorDetailItems> itemses = new ArrayList<>();
        HttpResponse httpResponse = postData(UrlList.PROFESSOR_GET_DETAIL_INFO + String.valueOf(majorType), null);
        JSONObject jsonObject = (JSONObject) jsonParser.parse(
                new InputStreamReader(httpResponse.getEntity().getContent()));
        if (checkResultData(jsonObject)) {
            JSONArray jsonArray = (JSONArray) jsonObject.get("data");
            for (Object dataObject : jsonArray) {
                JSONObject dataJsonObject = (JSONObject) dataObject;
                String name = URLDecode(dataJsonObject.get("name").toString());
                String major = URLDecode(dataJsonObject.get("major").toString());
                String phoneNumber = URLDecode(dataJsonObject.get("phone").toString());
                String email = URLDecode(dataJsonObject.get("email").toString());
                itemses.add(new MajorDetailItems(name, major, phoneNumber, email));
            }
        } else {
            return null;
        }
        return itemses;
    }

    public JSONObject getServerLibrary(String bookkeyword, String page) throws IOException, ParseException {
        ContentType contentType = ContentType.create("text/plain", Charset.forName("UTF-8"));
        HttpClient libraryHttpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(UrlList.SCHOOL_SERVER_LIBRARY_URL);
        MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
        entityBuilder.addTextBody("p", page, contentType);
        entityBuilder.addTextBody("q", bookkeyword, contentType);

        CookieStore cookieStore = ((DefaultHttpClient) libraryHttpClient).getCookieStore();
        String[] cookieName = new String[]{"mast_idno", "mast_mjco", "mast_name", "mast_name_e",
                "mast_pass", "user_auto", "user_gubn"};
        String[] cookieValue = new String[]{"MjAxNDAxMjM5", "", "", "", "", "", "\"MQ==\""};

        for (int i = 0; i < cookieName.length; i++) {
            BasicClientCookie cookie = new BasicClientCookie(cookieName[i], cookieValue[i]);
            cookie.setDomain("m.kangnam.ac.kr");
            cookie.setPath("/");
            cookieStore.addCookie(cookie);
        }
        httpPost.setEntity(entityBuilder.build());
        HttpResponse httpResponse = libraryHttpClient.execute(httpPost);
        JSONParser jsonParser = new JSONParser();
        return (JSONObject) jsonParser.parse(new InputStreamReader(httpResponse.getEntity().getContent()));
    }

    public ArrayList<SchoolRestrauntItems> getRestrauntInfo(SchoolRestraunt restraunt) throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        ArrayList<SchoolRestrauntItems> itemses = new ArrayList<>();
        HttpResponse httpResponse = postData(UrlList.SCHOOL_RESTRAUNT_INFO + restraunt.name().toLowerCase(), null);
        JSONObject jsonObject = (JSONObject) jsonParser.parse(
                new InputStreamReader(httpResponse.getEntity().getContent()));
        if (checkResultData(jsonObject)) {
            JSONArray jsonArray = (JSONArray) jsonObject.get("data");
            for (Object object : jsonArray) {
                JSONObject object1 = (JSONObject) object;
                String foodName = URLDecode(object1.get("foodname").toString());
                String foodPrice = URLDecode(object1.get("foodprice").toString());
                itemses.add(new SchoolRestrauntItems(foodName, foodPrice));
            }
        }
        return itemses;
    }

    public ArrayList<LibrarySeatItems> getLibrarySeatInfo() throws IOException {
        ArrayList<LibrarySeatItems> itemses = new ArrayList<>();
        URL url = new URL(UrlList.LIBRARY_SEAT_URL);
        Document document = Jsoup.parse(url, 10000);
        Elements elements = document.getElementsByTag("tr");
        /*
            (0 || 1 || 2) 은 무시할 정보 -- for 문 index 를 2부터
         */
        for (int i = 3; i < elements.size(); i++) {
            Elements innerElements = elements.get(i).getElementsByTag("td");
            int total = parseInt(innerElements.get(2).text());
            int useSeat = parseInt(innerElements.get(3).text());
            int emptySeat = total - useSeat;
            itemses.add(new LibrarySeatItems(total, useSeat, emptySeat));
        }
        return itemses;
    }

    public ArrayList<MajorSimpleListItems> getMajorSimpleInfo() throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        ArrayList<MajorSimpleListItems> itemses = new ArrayList<>();
        HttpResponse httpResponse = postData(UrlList.MAJOR_GET_SIMPLE_INFO, null);
        JSONObject jsonObject = (JSONObject) jsonParser.parse(
                new InputStreamReader(httpResponse.getEntity().getContent()));
        if (checkResultData(jsonObject)) {
            JSONArray jsonArray = (JSONArray) jsonObject.get("data");
            for (Object object : jsonArray) {
                JSONObject jObject = (JSONObject) object;
                String majorType = URLDecoder.decode(jObject.get("majorType").toString(), "UTF-8");
                String majorName = URLDecoder.decode(jObject.get("majorName").toString(), "UTF-8");
                String majorHomepage = URLDecoder.decode(jObject.get("majorHomepage").toString(), "UTF-8");
                itemses.add(new MajorSimpleListItems(majorName, majorHomepage, majorType));
            }
        }
        return itemses;
    }

    public JSONObject deleteAccount(String password) throws IOException, ParseException {
        HashMap<String, String> paramter = new HashMap<>();
        paramter.put("password", password);
        HttpResponse httpResponse = postData(UrlList.DELETE_ACCOUNT_URL, paramter);
        JSONParser jsonParser = new JSONParser();
        return (JSONObject) jsonParser.parse(new InputStreamReader(httpResponse.getEntity().getContent()));
    }

    public JSONObject getCouncilInfo() throws IOException, ParseException {
        HttpResponse httpResponse = postData(UrlList.STUDENT_GET_COUNCIL_INFO, null);
        JSONParser jsonParser = new JSONParser();
        return (JSONObject) jsonParser.parse(
                new InputStreamReader(httpResponse.getEntity().getContent()));
    }

    public JSONObject getCouncilInfoDetail(String contentid) throws IOException, ParseException {
        HashMap<String, String> parameter = new HashMap<>();
        parameter.put("contentid", contentid);
        HttpResponse httpResponse = getData(UrlList.STUDENT_GET_COUNCIL_DETAIL_INFO, parameter);
        JSONParser jsonParser = new JSONParser();
        return (JSONObject) jsonParser.parse(
                new InputStreamReader(httpResponse.getEntity().getContent()));
    }

    public JSONObject getCouncilWelfareInfo() throws IOException, ParseException {
        HttpResponse httpResponse = postData(UrlList.STUDENT_COUNCIL_GET_WELFARE, null);
        JSONParser jsonParser = new JSONParser();
        return (JSONObject) jsonParser.parse(
                new InputStreamReader(httpResponse.getEntity().getContent()));
    }

    public boolean writeMeetingContent(String content, String schoolname, String majorname,
                                       String studentcount, String gender) throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        HashMap<String, String> parameter = new HashMap<>();
        parameter.put("content", content);
        parameter.put("schoolname", schoolname);
        parameter.put("majorname", majorname);
        parameter.put("studentcount", studentcount);
        parameter.put("gender", gender);
        HttpResponse httpResponse = postData(UrlList.WRITE_MEETING_BOARD_LIST, parameter);
        JSONObject jsonObject = (JSONObject) jsonParser.parse(
                new InputStreamReader(httpResponse.getEntity().getContent()));
        return checkResultData(jsonObject);
    }

    public ArrayList<MeetingListItems> getMeetingBoardList(int page) throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        ArrayList<MeetingListItems> itemses = new ArrayList<>();

        HttpResponse httpResponse = postData(UrlList.MEETING_BOARD_GET_LIST + page, null);
        JSONObject jsonObject = (JSONObject) jsonParser.parse(
                new InputStreamReader(httpResponse.getEntity().getContent())
        );

        if (!checkResultData(jsonObject)) {
            return null;
        }

        JSONArray jsonArray = (JSONArray) jsonObject.get("data");

        for (Object obj : jsonArray) {
            JSONObject object = (JSONObject) obj;
            MeetingListItems.TYPE type = null;

            String matchingResult = object.get("matchingresult").toString();
            String gender = object.get("gender").toString();
//            String content = object.get("content").toString();
            String schoolName = object.get("schoolname").toString();
            String majorName = object.get("majorname").toString();
            String time = object.get("time").toString();
            String replyCount = object.get("commentcount").toString();
            String peopleCount = object.get("studentcount").toString();
            String contentid = object.get("contentid").toString();
            String writer = object.get("writer").toString();
            String studenuName = object.get("studentname").toString();

            if (gender.equals("male")) {
                type = MeetingListItems.TYPE.BOY_GROUP;
            } else if (gender.equals("female")) {
                type = MeetingListItems.TYPE.GIRL_GROUP;
            }
            if (matchingResult.equals("1")) {
                type = MeetingListItems.TYPE.SUCCESS_GROUP;
            }

            itemses.add(new MeetingListItems(Integer.parseInt(contentid), type, schoolName, majorName, time,
                    Integer.parseInt(replyCount), Integer.parseInt(peopleCount), gender, writer, studenuName));
        }
        return itemses;
    }

    public JSONObject writeBoardContent(int boardType, String title, String content, HashMap<String, Uri> file,
                                        boolean isEditMode, String contentid, int category) throws IOException, ParseException, JSONException {
        JSONParser jsonParser = new JSONParser();
        MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
        HttpResponse httpResponse;

        multipartEntityBuilder.addTextBody("title", title, getDefaultContentType());
        multipartEntityBuilder.addTextBody("content", content, getDefaultContentType());
        multipartEntityBuilder.addTextBody("isEditmode", String.valueOf(isEditMode));
        multipartEntityBuilder.addTextBody("category", String.valueOf(category));

        /* 수정 모드일 경우 콘텐트 아이디 post */
        if (isEditMode) {
            multipartEntityBuilder.addTextBody("contentID", contentid);
        }

        if (file.size() > 0) {
            Set<String> keySet = file.keySet();
            Iterator<String> key = keySet.iterator();
            org.json.JSONObject fileObject = new org.json.JSONObject();
            org.json.JSONArray fileArray = new org.json.JSONArray();

            for (int i = 0; key.hasNext(); i++) {
                String realPath;
                String strKey = key.next();
                Uri value;
                File realPathFile;

                if ((value = file.get(strKey)) != null) {
                    realPath = ApplicationUtil.getInstance().UriToPath(value);
                    realPathFile = new File(realPath);
                    multipartEntityBuilder.addBinaryBody("file[" + i + "]", realPathFile);
                    log("file[" + i + "] :  " + realPath);
                } else {
                    i--;
                    fileArray.put(strKey);
                }
                fileObject.put("existfile", fileArray);
            }
            multipartEntityBuilder.addTextBody("existfile", fileObject.toString());
            log("already Exist : " + fileObject.toString());
        }

        if (boardType == -1) {
            return null;
        }

        HttpPost httpPost = new HttpPost(UrlList.BOARD_WRITE_CONTENT + boardType);
        httpPost.setEntity(multipartEntityBuilder.build());
        httpResponse = httpClient.execute(httpPost);
        return (JSONObject) jsonParser.parse(
                new InputStreamReader(httpResponse.getEntity().getContent())
        );
    }

    public JSONObject writeDelivery(int deliveryFoodCategory, String shopname, String phonenum,
                                    HashMap<String, Uri> file) throws IOException, ParseException, JSONException {
        JSONParser jsonParser = new JSONParser();
        MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
        HttpResponse httpResponse;

        multipartEntityBuilder.addTextBody("category", String.valueOf(deliveryFoodCategory), getDefaultContentType());
        multipartEntityBuilder.addTextBody("shopname", shopname, getDefaultContentType());
        multipartEntityBuilder.addTextBody("phone", phonenum);

        if (file.size() > 0) {
            Set<String> keySet = file.keySet();
            Iterator<String> key = keySet.iterator();
            org.json.JSONObject fileObject = new org.json.JSONObject();
            org.json.JSONArray fileArray = new org.json.JSONArray();

            for (int i = 0; key.hasNext(); i++) {
                String realPath;
                String strKey = key.next();
                Uri value;
                File realPathFile;

                if ((value = file.get(strKey)) != null) {
                    realPath = ApplicationUtil.getInstance().UriToPath(value);
                    realPathFile = new File(realPath);
                    multipartEntityBuilder.addBinaryBody("file[" + i + "]", realPathFile);
                    log("file[" + i + "] :  " + realPath);
                } else {
                    i--;
                    fileArray.put(strKey);
                }
                fileObject.put("existfile", fileArray);
            }
            multipartEntityBuilder.addTextBody("existfile", fileObject.toString());
            log("already Exist : " + fileObject.toString());
        }

        HttpPost httpPost = new HttpPost(UrlList.DELIVERY_BOARD_WRITE);
        httpPost.setEntity(multipartEntityBuilder.build());
        httpResponse = httpClient.execute(httpPost);
        return (JSONObject) jsonParser.parse(
                new InputStreamReader(httpResponse.getEntity().getContent())
        );
    }

    private ArrayList<NoticeItems> noticeParser(NoticeType noticeType, int timeout) throws IOException {
        ArrayList<NoticeItems> itemses = new ArrayList<>();
        String id = "", title = "", time = "", readcount = "", url = "";
        String BASE_URL = null, PARSE_URL = null;

        if (noticeType == NoticeType.NOTICE) {
            BASE_URL = "http://web.kangnam.ac.kr/plaza/infom/notice/";
            PARSE_URL = BASE_URL + "notice_list.jsp";
        } else if (noticeType == NoticeType.HAKSA) {
            BASE_URL = "http://web.kangnam.ac.kr/plaza/infom/eduinfo/";
            PARSE_URL = BASE_URL + "eduinfo_list.jsp";
        } else if (noticeType == NoticeType.JANGHAK) {
            BASE_URL = "http://web.kangnam.ac.kr/plaza/infom/scholar/";
            PARSE_URL = BASE_URL + "scholar_list.jsp";
        }

        Document document = Jsoup.parse(new URL(PARSE_URL), timeout);

        Elements elements = document.getElementsByClass("boardList");
        Elements tableBodyElements = elements.get(0).getElementsByTag("tbody")
                .get(0).getElementsByTag("tr");
        for (Element tableElement : tableBodyElements) {
            int i = 0;
            for (Element tableDocument : tableElement.getElementsByTag("td")) {
                switch (i) {
                    case 0:
                        id = tableDocument.text();
                        break;
                    case 1:
                        title = tableDocument.text();
                        url = BASE_URL + tableDocument.select("a").first().attr("href");
                        break;
                    case 2:
                        time = tableDocument.text();
                        break;
                    case 3:
                        readcount = tableDocument.text();
                        break;
                }
                i++;
            }
            itemses.add(new NoticeItems(id, title, url, time, readcount));
        }
        return itemses;
    }


    public HashMap<String, ArrayList<NoticeItems>> getNoticeList() throws IOException {
        HashMap<String, ArrayList<NoticeItems>> itemes = new HashMap<>();
        itemes.put("notice", noticeParser(NoticeType.NOTICE, 5000));
        itemes.put("haksa", noticeParser(NoticeType.HAKSA, 5000));
        itemes.put("janghak", noticeParser(NoticeType.JANGHAK, 5000));
        return itemes;
    }

    public ArrayList<DeliveryListItems> getDeliveryFoodList(int page, int foodtype, String content) throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        HttpResponse httpResponse;
        HashMap<String, String> parameter = new HashMap<>();
        ArrayList<DeliveryListItems> datalist = new ArrayList<>();

        parameter.put("foodtype", String.valueOf(foodtype));
        if (content != null) {
            parameter.put("content", content);
        }
        httpResponse = postData(UrlList.DELIVERY_GET_LIST + page, parameter);
        JSONObject jsonObject = (JSONObject) jsonParser.parse(new InputStreamReader(httpResponse.getEntity().getContent()));

        if (checkResultData(jsonObject)) {
            JSONArray jsonArray = (JSONArray) jsonObject.get("data");
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject dataObject = (JSONObject) jsonArray.get(i);
                String name = dataObject.get("name").toString();
                String phone = dataObject.get("phone").toString();
                String imagePath = dataObject.get("imagepath").toString();
                datalist.add(new DeliveryListItems(name, phone, imagePath));
            }
            return datalist;
        } else {
            return null;
        }
    }

    public ArrayList<DefaultBoardListItems> getDefaultboardList(BoardType boardType, int page, String content,
                                                                int category) throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        HashMap<String, String> parameter = new HashMap<>();
        HttpResponse httpResponse;

        // 검색할 경우 Content 파라미터에 검색을 위한 텍스트가 넘어옴
        if (content != null) {
            parameter.put("content", content);
        }
        // 카테고리 인덱스 값을 넣어줌
        parameter.put("category", String.valueOf(category));

        if (boardType == BoardType.FREE) {
            httpResponse = postData(UrlList.FREEBOARD_GET_LIST + page, parameter);
        } else if (boardType == BoardType.FAQ) {
            httpResponse = postData(UrlList.FAQBOARD_GET_LIST + page, parameter);
        } else if (boardType == BoardType.GREENLIGHT) {
            httpResponse = postData(UrlList.GREENLIGHT_GET_LIST + page, parameter);
        } else if (boardType == BoardType.MARKET) {
            httpResponse = postData(UrlList.MARKET_GET_LIST + page, parameter);
        } else {
            return null;
        }

        JSONObject object = (JSONObject) jsonParser.parse(
                new InputStreamReader(httpResponse.getEntity().getContent()));
        if (!checkResultData(object)) {
            return null;
        }
        ArrayList<DefaultBoardListItems> list = new ArrayList<>();
        JSONArray jsonArray = (JSONArray) object.get("data");
        for (Object obj : jsonArray) {
            JSONObject jsonObject = (JSONObject) obj;
            String studentnumber = jsonObject.get("studentnumber").toString();
            String title = jsonObject.get("title").toString();
            String contentid = jsonObject.get("contentid").toString();
            String writername = jsonObject.get("writername").toString();
            String time = jsonObject.get("time").toString();
            String commentsize = jsonObject.get("commentsize").toString();
            list.add(new DefaultBoardListItems(title, studentnumber, contentid, writername, time, Integer.parseInt(commentsize)));
        }
        return list;
    }

    public JSONObject getDefaultboardContent(String contentID) throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        HttpResponse httpResponse = postData(UrlList.DEFAULTBOARD_GET_CONTENT + contentID, null);
        JSONObject object = (JSONObject) jsonParser.parse(
                new InputStreamReader(httpResponse.getEntity().getContent()));
        return object;
    }

    /**
     * @param contentID greenLight Board Content ID
     * @throws IOException
     * @throws ParseException
     */
    public JSONObject getGreenLightResult(String contentID) throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        HashMap<String, String> parameter = new HashMap<>();
        HttpResponse httpResponse = postData(UrlList.GET_GREENLIGHT_RESULT + contentID, parameter);
        return (JSONObject) jsonParser.parse(
                new InputStreamReader(httpResponse.getEntity().getContent()));
    }

    public JSONObject setGreenLightResult(String contentID, boolean isOn) throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        HashMap<String, String> parameter = new HashMap<>();
        parameter.put("result", isOn ? "1" : "0");
        HttpResponse httpResponse = postData(UrlList.SET_GREENLIGHT_RESULT + contentID, parameter);
        return (JSONObject) jsonParser.parse(
                new InputStreamReader(httpResponse.getEntity().getContent()));
    }

    public ArrayList<CommentListItems> getComment(String contentID) throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        HttpResponse httpResponse = postData(UrlList.GET_COMMENT + contentID, null);
        JSONObject object = (JSONObject) jsonParser.parse(
                new InputStreamReader(httpResponse.getEntity().getContent()));
        if (!checkResultData(object)) {
            return null;
        }
        ArrayList<CommentListItems> list = new ArrayList<>();
        JSONArray jsonArray = (JSONArray) object.get("data");
        for (Object obj : jsonArray) {
            JSONObject jsonObject = (JSONObject) obj;
            String commentid = jsonObject.get("commentid").toString();
            String studentnumber = jsonObject.get("studentnumber").toString();
            String writername = jsonObject.get("name").toString();
            String time = jsonObject.get("time").toString();
            String comment = jsonObject.get("content").toString();
            list.add(new CommentListItems(commentid, writername, comment, studentnumber, time));
        }
        return list;
    }

    public JSONObject deleteComment(String commentid) throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        HashMap<String, String> parameter = new HashMap<>();
        parameter.put("commentid", commentid);
        HttpResponse httpResponse = postData(UrlList.DELETE_BOARD_COMMENT_URL, parameter);
        return (JSONObject) jsonParser.parse(new InputStreamReader(httpResponse.getEntity().getContent()));
    }

    public boolean writeComment(String contentID, String comment) throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        HashMap<String, String> parameter = new HashMap<>();
        parameter.put("comment", comment);
        HttpResponse httpResponse = postData(UrlList.WRITE_COMMENT + contentID, parameter);
        JSONObject object = (JSONObject) jsonParser.parse(
                new InputStreamReader(httpResponse.getEntity().getContent())
        );
        return checkResultData(object);
    }

    public JSONObject deleteBoardList(String contentID) throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        HashMap<String, String> parameter = new HashMap<>();
        HttpResponse httpResponse = postData(UrlList.DELETE_BOARD_LIST + contentID, parameter);
        return (JSONObject) jsonParser.parse(
                new InputStreamReader(httpResponse.getEntity().getContent())
        );
    }

    public ArrayList<ShareTaxiListItems> getShareTaxiList(String date, int page) throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        HashMap<String, String> parameter = new HashMap<>();
        parameter.put("time", date);
        HttpResponse httpResponse = postData(UrlList.GET_SHARETAXI_URL + page, parameter);
        JSONObject jsonObject = (JSONObject) jsonParser.parse(new InputStreamReader(httpResponse.getEntity().getContent()));

        if (jsonObject == null) {
            return null;
        }

        if (checkResultData(jsonObject)) {
            ArrayList<ShareTaxiListItems> itemses = new ArrayList<>();
            JSONArray rootArray = (JSONArray) jsonObject.get("data");
            for (Object rootObject : rootArray) {
                JSONObject rootJsonObject = (JSONObject) rootObject;
                String writer = rootJsonObject.get("writer").toString();
                String isLeave = rootJsonObject.get("isLeave").toString();
                String departuretime = rootJsonObject.get("departuretime").toString();
                String destination = rootJsonObject.get("destination").toString();
                String departure = rootJsonObject.get("departure").toString();
                String contentid = rootJsonObject.get("contentid").toString();
                JSONArray sharePersonArray = (JSONArray) rootJsonObject.get("shareperson");
                int personSize = sharePersonArray.size();
                String[] personArray = new String[personSize];

                for (int i = 0; i < personSize; i++) {
                    personArray[i] = sharePersonArray.get(i).toString();
                }

                itemses.add(new ShareTaxiListItems(contentid, writer, isLeave, departuretime,
                        destination, departure, personArray));
            }
            return itemses;
        } else {
            return null;
        }
    }

    public JSONObject getShareTaxiContent(String contentid) throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        HashMap<String, String> parameter = new HashMap<>();
        HttpResponse httpResponse = postData(UrlList.GET_SHARETAXI_CONTENT_URL + contentid, parameter);
        return (JSONObject) jsonParser.parse(new InputStreamReader(httpResponse.getEntity().getContent()));
    }


    /**
     * @param contentid board content id
     * @param isLeave   "0" is Not Leave
     *                  "1" is Leave
     * @return resultObject
     */
    public JSONObject setShareTaxiLeave(String contentid, String isLeave) throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        HashMap<String, String> parameter = new HashMap<>();
        parameter.put("leave", isLeave);
        parameter.put("contentid", contentid);
        HttpResponse httpResponse = postData(UrlList.SET_SHARETAXI_LEAVE_URL, parameter);
        return (JSONObject) jsonParser.parse(new InputStreamReader(httpResponse.getEntity().getContent()));
    }

    /**
     * @param contentid board content id
     * @param isWith    "0" is not with
     *                  "1" is with
     * @return resultObject
     */
    public JSONObject setShareTaxiWith(String contentid, String isWith) throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        HashMap<String, String> parameter = new HashMap<>();
        parameter.put("with", isWith);
        parameter.put("contentid", contentid);
        HttpResponse httpResponse = postData(UrlList.SET_SHARETAXI_WITH_URL, parameter);
        return (JSONObject) jsonParser.parse(new InputStreamReader(httpResponse.getEntity().getContent()));
    }

    public JSONObject writeShareTaxiBoard(String departuretime, String departure, String destination,
                                          String peopleCount, String content) throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        HashMap<String, String> parameter = new HashMap<>();
        parameter.put("departuretime", departuretime);
        parameter.put("departure", departure);
        parameter.put("destination", destination);
        parameter.put("peoplecount", peopleCount);
        parameter.put("content", content);
        HttpResponse httpResponse = postData(UrlList.WRITE_SHARETAXI_URL, parameter);
        return (JSONObject) jsonParser.parse(
                new InputStreamReader(httpResponse.getEntity().getContent())
        );
    }

    public JSONObject registerPhoneNumber(String phonenumber) throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        HashMap<String, String> parameter = new HashMap<>();
        parameter.put("phone", phonenumber);
        HttpResponse httpResponse = postData(UrlList.PHONE_REGISTER_URL, parameter);
        return (JSONObject) jsonParser.parse(
                new InputStreamReader(httpResponse.getEntity().getContent())
        );
    }

    /**
     * @return getPhoneNumber Data as JSONObject
     * if success to get phone number
     * {result="success", "data"="phonenumber"}
     * if fail to get phone number
     * {result="fail", "reason"="cause"}
     * @throws IOException
     * @throws ParseException
     */
    public JSONObject getPhoneNumber() throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        HttpResponse httpResponse = postData(UrlList.GET_PHONE_NUMBER_URL, null);
        return (JSONObject) jsonParser.parse(
                new InputStreamReader(httpResponse.getEntity().getContent())
        );
    }

    public JSONObject getLooknLook() throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        HttpResponse httpResponse = getData(UrlList.BEAT_LOOKNLOOK_URL);
        return (JSONObject) jsonParser.parse(
                new InputStreamReader(httpResponse.getEntity().getContent())
        );
    }

    public JSONObject getBeatCulture() throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        HttpResponse httpResponse = getData(UrlList.BEAT_CULTURE_URL);
        return (JSONObject) jsonParser.parse(
                new InputStreamReader(httpResponse.getEntity().getContent())
        );
    }

    public JSONObject getBeatWelfare() throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        HttpResponse httpResponse = getData(UrlList.BEAT_WELFARE_URL);
        return (JSONObject) jsonParser.parse(
                new InputStreamReader(httpResponse.getEntity().getContent())
        );
    }

    public JSONObject getBeatReview() throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        HttpResponse httpResponse = getData(UrlList.BEAT_REVIEW_URL);
        return (JSONObject) jsonParser.parse(
                new InputStreamReader(httpResponse.getEntity().getContent())
        );
    }

    public JSONObject getBeatEtc() throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        HttpResponse httpResponse = getData(UrlList.BEAT_ETC_URL);
        return (JSONObject) jsonParser.parse(
                new InputStreamReader(httpResponse.getEntity().getContent())
        );
    }

    public JSONObject getBeatQna() throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        HttpResponse httpResponse = getData(UrlList.BEAT_QNA_URL);
        return (JSONObject) jsonParser.parse(
                new InputStreamReader(httpResponse.getEntity().getContent())
        );
    }

    public JSONObject getBeatDetail(HashMap<String, String> parameter) throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        HttpResponse httpResponse = getData(UrlList.BEAT_DETAIL_URL, parameter);
        return (JSONObject) jsonParser.parse(
                new InputStreamReader(httpResponse.getEntity().getContent())
        );
    }

    public JSONObject deleteBeatDetail(HashMap<String, String> parameter) throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        HttpResponse httpResponse = getData(UrlList.BEAT_DETAIL_DELETE_URL, parameter);
        return (JSONObject) jsonParser.parse(
                new InputStreamReader(httpResponse.getEntity().getContent())
        );
    }

    public JSONObject writeBeatContent(int boardType, String title, String content, HashMap<String, Uri> file,
                                       boolean isEditMode, String contentid) throws IOException, ParseException, JSONException {
        JSONParser jsonParser = new JSONParser();
        MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
        HttpResponse httpResponse;

        multipartEntityBuilder.addTextBody("title", title, getDefaultContentType());
        multipartEntityBuilder.addTextBody("content", content, getDefaultContentType());
        multipartEntityBuilder.addTextBody("isEditmode", String.valueOf(isEditMode));

        /* 수정 모드일 경우 콘텐트 아이디 post */
        if (isEditMode) {
            multipartEntityBuilder.addTextBody("contentID", contentid);
        }

        if (file.size() > 0) {
            Set<String> keySet = file.keySet();
            Iterator<String> key = keySet.iterator();
            org.json.JSONObject fileObject = new org.json.JSONObject();
            org.json.JSONArray fileArray = new org.json.JSONArray();

            for (int i = 0; key.hasNext(); i++) {
                String realPath;
                String strKey = key.next();
                Uri value;
                File realPathFile;

                if ((value = file.get(strKey)) != null) {
                    realPath = ApplicationUtil.getInstance().UriToPath(value);
                    realPathFile = new File(realPath);
                    multipartEntityBuilder.addBinaryBody("file[" + i + "]", realPathFile);
                    log("file[" + i + "] :  " + realPath);
                } else {
                    i--;
                    fileArray.put(strKey);
                }
                fileObject.put("existfile", fileArray);
            }
            multipartEntityBuilder.addTextBody("existfile", fileObject.toString());
            log("already Exist : " + fileObject.toString());
        }

        if (boardType == -1) {
            return null;
        }

        HttpPost httpPost = new HttpPost(UrlList.BEAT_WRITE_CONTENT_URL + boardType);
        httpPost.setEntity(multipartEntityBuilder.build());
        httpResponse = httpClient.execute(httpPost);
        return (JSONObject) jsonParser.parse(
                new InputStreamReader(httpResponse.getEntity().getContent())
        );
    }

    public ArrayList<CommentListItems> getBeatComment(String contentID) throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        HttpResponse httpResponse = postData(UrlList.BEAT_GET_COMMENT_URL + contentID, null);
        JSONObject object = (JSONObject) jsonParser.parse(
                new InputStreamReader(httpResponse.getEntity().getContent()));
        if (!checkResultData(object)) {
            return null;
        }
        ArrayList<CommentListItems> list = new ArrayList<>();
        JSONArray jsonArray = (JSONArray) object.get("data");
        for (Object obj : jsonArray) {
            JSONObject jsonObject = (JSONObject) obj;
            String commentid = jsonObject.get("commentid").toString();
            String studentnumber = jsonObject.get("studentnumber").toString();
            String writername = jsonObject.get("name").toString();
            String time = jsonObject.get("time").toString();
            String comment = jsonObject.get("content").toString();
            list.add(new CommentListItems(commentid, writername, comment, studentnumber, time));
        }
        return list;
    }

    public JSONObject deleteBeatComment(String commentid) throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        HashMap<String, String> parameter = new HashMap<>();
        parameter.put("commentid", commentid);
        HttpResponse httpResponse = postData(UrlList.BEAT_DELETE_COMMENT_URL, parameter);
        return (JSONObject) jsonParser.parse(new InputStreamReader(httpResponse.getEntity().getContent()));
    }

    public boolean writeBeatComment(String contentID, String comment) throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        HashMap<String, String> parameter = new HashMap<>();
        parameter.put("comment", comment);
        HttpResponse httpResponse = postData(UrlList.BEAT_WRITE_COMMENT_URL + contentID, parameter);
        JSONObject object = (JSONObject) jsonParser.parse(
                new InputStreamReader(httpResponse.getEntity().getContent())
        );
        return checkResultData(object);
    }

    public JSONObject getSchoolClubInfo() throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        HttpResponse httpResponse = getData(UrlList.GET_SCHOOL_CLUB_INFO);
        return (JSONObject) jsonParser.parse(
                new InputStreamReader(httpResponse.getEntity().getContent())
        );
    }

    public JSONObject getSchoolClubDetail(String clubid) throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        HashMap<String, String> parameter = new HashMap<>();
        parameter.put("clubid", clubid);
        HttpResponse httpResponse = getData(UrlList.GET_SCHOOL_CLUB_DETAIL, parameter);
        return (JSONObject) jsonParser.parse(
                new InputStreamReader(httpResponse.getEntity().getContent())
        );
    }

    public String getProfileThumbURL(String studentnumber) {
        String format = "%s?type=thumbnail&studentnumber=%s";
        return String.format(format, UrlList.PROFILE_THUMB_IMAGE_URL, studentnumber);
    }

    public String getDeveloperProfileThumbURL(String name) {
        String format = "%s?name=%s";
        return String.format(format, UrlList.DEVELOPER_PROFILE_THUMB_IMAGE_URL, name);
    }

    private String URLEncode(String str) {
        try {
            return URLEncoder.encode(str, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
            return str;
        }
    }

    private String URLDecode(String str) throws UnsupportedEncodingException {
        return URLDecoder.decode(str, "UTF-8");
    }

    private boolean checkResultData(JSONObject jsonObject) {
        return jsonObject.get("result").equals("success");
    }

    private HttpResponse getData(String URL) throws IOException {
        return getData(URL, new HashMap<String, String>());
    }

    private HttpResponse getData(String URL, HashMap<String, String> parameter) throws IOException {
        Set<String> strings = parameter.keySet();
        String[] keyArray = strings.toArray(new String[strings.size()]);

        for (int i = 0; i < keyArray.length; i++) {
            if (i == 0) {
                URL = URL + "?" + keyArray[i] + "=" + URLEncode(parameter.get(keyArray[i]));
            } else {
                URL = URL + "&" + keyArray[i] + "=" + URLEncode(parameter.get(keyArray[i]));
            }
        }
        HttpGet httpGet = new HttpGet(URL);
        log("Get Url : " + URL);
        return httpClient.execute(httpGet);
    }

    private HttpResponse postData(String URL, HashMap<String, String> parameter) throws IOException {
        log("POST URL : " + URL);
        HttpPost httpPost = new HttpPost(URL);
        ContentType contentType = ContentType.create("text/plain", Charset.forName("UTF-8"));
        if (parameter != null) {
            MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
            Set<String> strings = parameter.keySet();
            String[] keyArray = strings.toArray(new String[strings.size()]);
            for (String key : keyArray) {
                entityBuilder.addTextBody(key, parameter.get(key), contentType);
                log("Entity Key : " + key);
                log("Entity Value : " + parameter.get(key));
            }
            httpPost.setEntity(entityBuilder.build());
        }

        return httpClient.execute(httpPost);
    }

    private ContentType getDefaultContentType() {
        return ContentType.create("text/plain", Charset.forName("UTF-8"));
    }

    private HashMap<String, String> getTextParameters(String[] keystrings, String[] valuestrings) {
        HashMap<String, String> hashMap = new HashMap<>();
        if (keystrings.length == valuestrings.length) {
            int size = keystrings.length;
            for (int i = 0; i < size; i++) {
                hashMap.put(keystrings[i], valuestrings[i]);
            }
        } else {
            log("getTextParameters key-value size error");
        }
        return hashMap;
    }

    private void log(String message) {
        String tag = getClass().getSimpleName();
        /* Release disable network util logging */
//        Log.d(tag, message);
    }

    public static enum SchoolRestraunt {SHAL, GYUNG, GISUK, INSA}

    public static enum LoginStatus {FAIL, NOMEMBER, SUCCESS, HASMEMBER, BANNICKNAME}

    private enum NoticeType {NOTICE, HAKSA, JANGHAK}

    public static enum BoardType {
        FREE(1), FAQ(2), GREENLIGHT(3), MEETING(4), SHARETAXT(5), MARKET(6);

        private final int num;

        private BoardType(int num) {
            this.num = num;
        }

        public int getValue() {
            return num;
        }

    }
}
