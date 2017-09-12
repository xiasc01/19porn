package cn.droidlover.xdroid.demo.model;

import android.widget.Button;

import java.util.List;

/**
 * Created by Administrator on 2017/2/12 0012.
 */

public class MovieInfo extends BaseModel {
    private List<Item> movies;

    public List<Item> getResults() {
        return movies;
    }

    public void setResults(List<Item> results) {
        this.movies = results;
    }

    public static class Item{
        private String id;
        private String movie_id;
        private String title;
        private String duration;
        private String value;
        private String media_url;
        private String media_pos;
        private String media_size;
        private String media_key;
        private String thumb_key;
        private String thumb_url;
        private String thumb_pos;
        private String thumb_size;
        private String type;
        private String set_name;
        private Boolean hasPlay = false;
        private String  score;
        private String  pic_score;
        private String  sub_type1;
        //private

        public String getMovie_id(){
            return movie_id;
        }

        public void setMovie_id(String movie_id){
            this.movie_id = movie_id;
        }

        public String getTitle(){
            return title;
        }

        public void setTitle(String title){
            this.title = title;
        }

        public String getDuration(){
            return duration;
        }

        public String getFormatDuration(){
            int time = Integer.parseInt(duration);
            int minus = time / 60;
            time %= 60;
            return String.format("%02d:%02d", minus, time);
        }

        public void setDuration(String duration){
            this.duration = duration;
        }

        public String getThumb_key() {
            return thumb_key;
        }

        public void setThumb_key(String thumb_key) {
            this.thumb_key = thumb_key;
        }

        public String getThumb_url() {
            return thumb_url;
        }

        public void setThumb_url(String thumb_url) {
            this.thumb_url = thumb_url;
        }

        public String getThumb_pos() {
            return thumb_pos;
        }

        public void setThumb_pos(String thumb_pos) {
            this.thumb_pos = thumb_pos;
        }

        public String getThumb_size() {
            return thumb_size;
        }

        public void setThumb_size(String thumb_size) {
            this.thumb_size = thumb_size;
        }

        public String getMedia_pos() {
            return media_pos;
        }

        public void setMedia_pos(String media_pos) {
            this.media_pos = media_pos;
        }

        public String getMedia_size() {
            return media_size;
        }

        public void setMedia_size(String media_size) {
            this.media_size = media_size;
        }

        public String getMedia_key() {
            return media_key;
        }

        public void setMedia_key(String media_key) {
            this.media_key = media_key;
        }

        public String getMedia_url() {
            return media_url;
        }

        public void setMedia_url(String media_url) {
            this.media_url = media_url;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Boolean getHasPlay() {
            return hasPlay;
        }

        public void setHasPlay(Boolean hasPlay) {
            this.hasPlay = hasPlay;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getSet_name() {
            return set_name;
        }

        public void setSet_name(String set_name) {
            this.set_name = set_name;
        }

        public String getScore() {
            return score;
        }

        public void setScore(String score) {
            this.score = score;
        }

        public String getPic_score() {
            return pic_score;
        }

        public void setPic_score(String pic_score) {
            this.pic_score = pic_score;
        }

        public String getSub_type1() {
            return sub_type1;
        }

        public void setSub_type1(String sub_type1) {
            this.sub_type1 = sub_type1;
        }
    }
}
