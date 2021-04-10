package lor.and.company.driver.models;

import android.os.Parcel;
import android.os.Parcelable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class Wallpaper implements Parcelable {
    String id, name, thumbnailLink, downloadLink, collection;
    long createdOn, size;

    public Wallpaper(String id, String name, String thumbnailLink, String downloadLink, String collection, long createdOn, long size) {
        this.id = id;
        this.name = name;
        this.thumbnailLink = thumbnailLink;
        this.downloadLink = downloadLink;
        this.collection = collection;
        this.createdOn = createdOn;
        this.size = size;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCollection() {
        return collection;
    }

    public long getCreatedOn() {
        return createdOn;
    }

    public String getThumbnailLink() {
        return thumbnailLink;
    }

    public String getDownloadLink() {
        return downloadLink;
    }

    public long getSize() {
        return size;
    }

    public Object[] createObject() {
        return new Object[]{this.id,this.name,this.thumbnailLink,this.downloadLink,this.collection,this.createdOn,this.size};
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeString(this.name);
        dest.writeString(this.thumbnailLink);
        dest.writeString(this.downloadLink);
        dest.writeString(this.collection);
        dest.writeLong(this.createdOn);
        dest.writeLong(this.size);
    }

    protected Wallpaper(Parcel in) {
        this.id = in.readString();
        this.name = in.readString();
        this.thumbnailLink = in.readString();
        this.downloadLink = in.readString();
        this.collection = in.readString();
        this.createdOn = in.readLong();
        this.size = in.readLong();
    }

    public static final Creator<Wallpaper> CREATOR = new Creator<Wallpaper>() {
        @Override
        public Wallpaper createFromParcel(Parcel source) {
            return new Wallpaper(source);
        }

        @Override
        public Wallpaper[] newArray(int size) {
            return new Wallpaper[size];
        }
    };
}
