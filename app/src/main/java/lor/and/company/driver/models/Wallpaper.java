package lor.and.company.driver.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.Keep;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class Wallpaper implements Parcelable {
    String id, name, thumbnailLink, downloadLink, collection;
    long createdOn, size, addedOn = 0;
    int x, y;
    boolean onFavourites = false;

    public Wallpaper(String id, String name, String thumbnailLink, String downloadLink, String collection, long createdOn, long size, int x, int y) {
        this.id = id;
        this.name = name;
        this.thumbnailLink = thumbnailLink;
        this.downloadLink = downloadLink;
        this.collection = collection;
        this.createdOn = createdOn;
        this.size = size;
        this.x = x;
        this.y = y;
    }

    public boolean isFavourite() {
        return onFavourites;
    }

    public void setFavourite(boolean onFavourites) {
        this.onFavourites = onFavourites;
    }

    public void setAddedOn(long addedOn) {
        this.addedOn = addedOn;
    }

    public long getAddedOn() {
        return addedOn;
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

    public long getSize() {
        return size;
    }

    public int getWidth() {
        return x;
    }

    public int getHeight() {
        return y;
    }

    public Object[] createObject() {
        return new Object[]{this.id,this.name,this.thumbnailLink,this.downloadLink,this.collection,this.createdOn,this.size,this.x,this.y};
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
        dest.writeInt(this.x);
        dest.writeInt(this.y);
    }

    protected Wallpaper(Parcel in) {
        this.id = in.readString();
        this.name = in.readString();
        this.thumbnailLink = in.readString();
        this.downloadLink = in.readString();
        this.collection = in.readString();
        this.createdOn = in.readLong();
        this.size = in.readLong();
        this.x = in.readInt();
        this.y = in.readInt();
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
