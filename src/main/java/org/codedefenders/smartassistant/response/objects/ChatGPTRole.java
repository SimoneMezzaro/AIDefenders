package org.codedefenders.smartassistant.response.objects;

import com.google.gson.annotations.SerializedName;

public enum ChatGPTRole {
    @SerializedName("system") SYSTEM,
    @SerializedName("user") USER,
    @SerializedName("assistant") ASSISTANT
}
