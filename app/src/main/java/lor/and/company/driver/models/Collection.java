package lor.and.company.driver.models;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class Collection implements Parcelable {
    String id, name, folderLink, owner, errorMessage;
    int count;
    long modifiedTime;
    Uri thumbnailLink;
    boolean error, selected = false;

    public Collection(String id, String name, String owner, String folderLink, long modifiedTime, int count, String thumbnailLink, boolean error, String errorMessage) {
        this.id = id;
        this.name = name;
        this.folderLink = folderLink;
        this.count = count;
        this.owner = owner;
        this.modifiedTime = modifiedTime;
        this.thumbnailLink = Uri.parse(thumbnailLink);
        this.error = error;
        this.errorMessage = errorMessage;
    }

    public Object[] createObject() {
        return new Object[]{this.id, this.name, this.owner, this.folderLink, this.count, this.modifiedTime, this.thumbnailLink, this.error, this.errorMessage};
    }

    protected Collection(Parcel in) {
        id = in.readString();
        name = in.readString();
        folderLink = in.readString();
        owner = in.readString();
        count = in.readInt();
        modifiedTime = in.readLong();
        thumbnailLink = in.readParcelable(Uri.class.getClassLoader());
        error = in.readByte() != 0;
        errorMessage = in.readString();
        selected = false;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeString(folderLink);
        dest.writeString(owner);
        dest.writeInt(count);
        dest.writeLong(modifiedTime);
        dest.writeParcelable(thumbnailLink, flags);
        dest.writeByte((byte) (error ? 1 : 0));
        dest.writeString(errorMessage);
    }



    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Collection> CREATOR = new Creator<Collection>() {
        @Override
        public Collection createFromParcel(Parcel in) {
            return new Collection(in);
        }

        @Override
        public Collection[] newArray(int size) {
            return new Collection[size];
        }
    };

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean getSelected(){
        return selected;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getOwner() {
        return owner;
    }

    public String getFolderLink() {
        return folderLink;
    }

    public long getModifiedTime() {
        return modifiedTime;
    }

    public Uri getThumbnailLink() {
        return thumbnailLink;
    }

    public boolean isError() {
        return error;
    }

    public int getCount() {
        return count;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
