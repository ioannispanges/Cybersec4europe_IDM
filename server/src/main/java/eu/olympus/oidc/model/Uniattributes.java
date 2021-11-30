package eu.olympus.oidc.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.olympus.util.Util;

public class Uniattributes {

    @JsonProperty("url:Name")
    private String name;
    @JsonProperty("url:DateOfBirth")
    private String dateOfBirth; // RFC compliant
    @JsonProperty("url:University")
    private String university;
    @JsonProperty("url:Awardeddegree")
    private String awardeddegree;
    @JsonProperty("url:Studentid")
    private String studentid;

    public Uniattributes(){}

    public Uniattributes(String name, String dateOfBirth, String university, String awardeddegree, String studentid) {
        this.name = name;
        this.dateOfBirth = Util.toRFC3339UTC(Util.fromRFC3339UTC(dateOfBirth));
        this.university = university;
        this.awardeddegree = awardeddegree;
        this.studentid = studentid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUniversity() {
        return university;
    }

    public void setUniversity(String university) {
        this.university = university;
    }

    public String getAwardeddegree() {
        return awardeddegree;
    }

    public void setAwardeddegree(String awardeddegree) {
        this.awardeddegree = awardeddegree;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = Util.toRFC3339UTC(Util.fromRFC3339UTC(dateOfBirth));
    }

    public String getStudentid() {
        return studentid;
    }

    public void setStudentid(String studentid) {
        this.studentid = studentid;
    }

    @Override
    public String toString() {
        return "Attributes {" + '\n' + '\t' +
                "name = " + name + "," + '\n' + '\t' +
                "dateOfBirth = " + dateOfBirth + "," + '\n' + '\t' +
                "university = " + university + "," + '\n' + '\t' +
                "awardeddegree = " + awardeddegree + '\n' + '\t' +
                "studentid = " + studentid + '\n' + '\t' +
                '}';
    }
}
