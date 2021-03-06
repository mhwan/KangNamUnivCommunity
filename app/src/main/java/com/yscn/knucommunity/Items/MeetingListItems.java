package com.yscn.knucommunity.Items;

/**
 * Created by GwonHyeok on 14. 11. 3..
 */
public class MeetingListItems {
    private String time, schoolname, majorname, gender, writer, studentname;
    private int replyCount, peopleCount, contentid;
    private TYPE type;

    public MeetingListItems(int contentid, TYPE type, String schoolname, String majorname,
                            String time, int replyCount, int peopleCount, String gender,
                            String writer, String studentname) {
        this.time = time;
        this.replyCount = replyCount;
        this.writer = writer;
        this.schoolname = schoolname;
        this.majorname = majorname;
        this.peopleCount = peopleCount;
        this.studentname = studentname;
        this.type = type;
        this.contentid = contentid;
        this.gender = gender;
    }

    public String getTime() {
        return time;
    }

    public int getReplyCount() {
        return replyCount;
    }

    public TYPE getType() {
        return type;
    }

    public int getPeopleCount() {
        return peopleCount;
    }

    public String getSchoolname() {
        return schoolname;
    }

    public String getMajorname() {
        return majorname;
    }

    public int getContentid() {
        return contentid;
    }

    public String getGender() {
        return gender;
    }

    public String getWriter() {
        return writer;
    }

    public String getStudentname() {
        return studentname;
    }

    public static enum TYPE {BOY_GROUP, GIRL_GROUP, SUCCESS_GROUP}
}
