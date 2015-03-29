/**
 * Copyright (C) 2015 Matthes Rieke
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.github.matthesrieke.realty.crawler;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.matthesrieke.realty.Ad;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Henning Bredel <h.bredel@52north.org>
 */
public class ImmobilienScout24Crawler implements Crawler {

	private static final Logger logger = LoggerFactory.getLogger(ImmobilienScout24Crawler.class);

    private static final String PROVIDER_NAME = "immobilienscout24";

    private static final String IS24_DATA = "IS24.resultList =";

    @Override
    public StringBuilder preprocessContent(StringBuilder content) {
        int startIndexIS24Data = content.indexOf(IS24_DATA);
        int endOfIS24Data = content.indexOf("};", startIndexIS24Data) + 1; // cut ;
        String data = content.substring(startIndexIS24Data + IS24_DATA.length(), endOfIS24Data);
        return new StringBuilder(data);
    }

    @Override
	public boolean supportsParsing(String url) {
        return url.startsWith("http://www.immobilienscout24.de");
    }

    @Override
    public List<Ad> parseDom(StringBuilder content) throws IOException {
        ObjectMapper om = new ObjectMapper();
        om.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

        Map<String,Object> results = om.readValue(content.toString(), Map.class);
        Map<String, Object> entries = (Map<String, Object>)results.get("model");
        List<Map<String, Object>> immo24Ads = (List<Map<String, Object>>) entries.get("results");

        List<Ad> ads = new ArrayList<Ad>();
        for (Map<String, Object> parsedAd : immo24Ads) {
            Immo24Ad immo24Ad = Immo24Ad.create(parsedAd);
            Ad ad = Ad.forId(String.valueOf(immo24Ad.getId()));
            ads.add(ad);
        }

        return ads;
    }

    @Override
	public int getFirstPageIndex() {
        return 1;
    }

    @Override
	public String prepareLinkForPage(String baseLink, int page) {
        return baseLink.replaceFirst("S-T/P-?/", "S-T/P-" + page + "/");
    }


    static class Immo24Ad {

        private int id;
        private String cwid;
        private boolean shortlisted;
        private boolean privateOffer;
        private String title;
        private String address;
        private String district;
        private String city;
        private String zip;
        private int mediaCount;
        private String[] pictureUrls;
        private double distanceInKm;
        private boolean hasNewFlag;
        private boolean hasUpdateFlag;
        private boolean hasFloorPlan;
        private boolean hasValuation;
        private String contactName;
        private AdProperties[] attributes;
        private String[] checkedAttributes;

        private Immo24Ad() {}

        public static Immo24Ad create(Map<String, Object> ad) {
            Immo24Ad immo24Ad = new Immo24Ad();
            immo24Ad.setAddress((String) ad.get("address"));
            immo24Ad.setCity((String) ad.get("city"));
            immo24Ad.setId((int) ad.get("id"));
            immo24Ad.setPictureUrls((String[]) ad.get("pictureUrls"));
            return immo24Ad;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getCwid() {
            return cwid;
        }

        public void setCwid(String cwid) {
            this.cwid = cwid;
        }

        public boolean isShortlisted() {
            return shortlisted;
        }

        public void setShortlisted(boolean shortlisted) {
            this.shortlisted = shortlisted;
        }

        public boolean isPrivateOffer() {
            return privateOffer;
        }

        public void setPrivateOffer(boolean privateOffer) {
            this.privateOffer = privateOffer;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getDistrict() {
            return district;
        }

        public void setDistrict(String district) {
            this.district = district;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getZip() {
            return zip;
        }

        public void setZip(String zip) {
            this.zip = zip;
        }

        public int getMediaCount() {
            return mediaCount;
        }

        public void setMediaCount(int mediaCount) {
            this.mediaCount = mediaCount;
        }

        public String[] getPictureUrls() {
            return pictureUrls;
        }

        public void setPictureUrls(String[] pictureUrls) {
            this.pictureUrls = pictureUrls;
        }

        public double getDistanceInKm() {
            return distanceInKm;
        }

        public void setDistanceInKm(double distanceInKm) {
            this.distanceInKm = distanceInKm;
        }

        public boolean isHasNewFlag() {
            return hasNewFlag;
        }

        public void setHasNewFlag(boolean hasNewFlag) {
            this.hasNewFlag = hasNewFlag;
        }

        public boolean isHasUpdateFlag() {
            return hasUpdateFlag;
        }

        public void setHasUpdateFlag(boolean hasUpdateFlag) {
            this.hasUpdateFlag = hasUpdateFlag;
        }

        public boolean isHasFloorPlan() {
            return hasFloorPlan;
        }

        public void setHasFloorPlan(boolean hasFloorPlan) {
            this.hasFloorPlan = hasFloorPlan;
        }

        public boolean isHasValuation() {
            return hasValuation;
        }

        public void setHasValuation(boolean hasValuation) {
            this.hasValuation = hasValuation;
        }

        public String getContactName() {
            return contactName;
        }

        public void setContactName(String contactName) {
            this.contactName = contactName;
        }

        public AdProperties[] getAttributes() {
            return attributes;
        }

        public void setAttributes(AdProperties[] attributes) {
            this.attributes = attributes;
        }

        public String[] getCheckedAttributes() {
            return checkedAttributes;
        }

        public void setCheckedAttributes(String[] checkedAttributes) {
            this.checkedAttributes = checkedAttributes;
        }


    }

    class AdProperties {
        private String title;
        private String value;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

    }

}
