package com.automation.healing.model;

import java.io.Serializable;

/**
 * Data model representing an extracted job listing from LinkedIn.
 */
public class JobDetails implements Serializable {

    private String title;
    private String company;
    private String location;
    private String datePosted;
    private String jobUrl;
    private String criteria; // e.g. "Full-time", "Entry level", "Remote"

    public JobDetails() {}

    public JobDetails(String title, String company, String location, String datePosted, String jobUrl) {
        this.title = title;
        this.company = company;
        this.location = location;
        this.datePosted = datePosted;
        this.jobUrl = jobUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDatePosted() {
        return datePosted;
    }

    public void setDatePosted(String datePosted) {
        this.datePosted = datePosted;
    }

    public String getJobUrl() {
        return jobUrl;
    }

    public void setJobUrl(String jobUrl) {
        this.jobUrl = jobUrl;
    }

    public String getCriteria() {
        return criteria;
    }

    public void setCriteria(String criteria) {
        this.criteria = criteria;
    }

    @Override
    public String toString() {
        return String.format("Job: '%s' | Company: '%s' | Location: '%s' | Posted: '%s'",
                title, company, location, datePosted);
    }
}
