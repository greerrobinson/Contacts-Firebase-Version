package capstone.defaultapps.contacts.Repository

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import android.util.Log

@Entity(tableName = "contactitems_table")
data class ContactItem(
    @PrimaryKey(autoGenerate = true) var id: Long? = null,
    @ColumnInfo(name= "uuid") var uuid:String = "",
    @ColumnInfo(name= "firstName") var firstName:String = "",
    @ColumnInfo(name= "lastName") var lastName:String = "",
    @ColumnInfo(name= "company") var company:String = "",
    @ColumnInfo(name= "cellPhone") var cellPhone:String = "",
    @ColumnInfo(name= "workPhone") var workPhone:String = "",
    @ColumnInfo(name= "email") var email:String = "",
    @ColumnInfo(name= "address") var address:String = "",
    @ColumnInfo(name= "notes") var notes:String = "",
)