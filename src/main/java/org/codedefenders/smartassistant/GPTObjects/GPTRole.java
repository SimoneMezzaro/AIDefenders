package org.codedefenders.smartassistant.GPTObjects;

import com.google.gson.annotations.SerializedName;

public enum GPTRole {
    @SerializedName("system") SYSTEM,
    @SerializedName("user") USER,
    @SerializedName("assistant") ASSISTANT
}
