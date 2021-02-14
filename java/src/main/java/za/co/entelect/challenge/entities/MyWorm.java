package za.co.entelect.challenge.entities;

import com.google.gson.annotations.SerializedName;

import org.graalvm.compiler.serviceprovider.ServiceProvider;

public class MyWorm extends Worm {
    @SerializedName("weapon")
    public Weapon weapon;

    @SerializedName("bananaBombs")
    public BananaBombs bananaBombs;

    @SerializedName("snowballs")
    public Snowballs snowballs;
}
