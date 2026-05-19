package dev.langchain4j.model.openai.internal.chat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.List;
import java.util.Objects;

@JsonDeserialize(builder = Delta.Builder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public final class Delta {

    @JsonProperty
    private final String role;
    @JsonProperty
    private final String content;
    @JsonProperty
    private final List<ToolCall> toolCalls;
    @JsonProperty
    @Deprecated
    private final FunctionCall functionCall;
    @JsonProperty("reasoning_content")
    private final String reasoningContent;

    public Delta(Builder builder) {
        this.role = builder.role;
        this.content = builder.content;
        this.toolCalls = builder.toolCalls;
        this.functionCall = builder.functionCall;
        this.reasoningContent = builder.reasoningContent;
    }

    public String role() {
        return role;
    }

    public String content() {
        return content;
    }

    public List<ToolCall> toolCalls() {
        return toolCalls;
    }

    @Deprecated
    public FunctionCall functionCall() {
        return functionCall;
    }

    public String reasoningContent() {
        return reasoningContent;
    }

    @Override
    public boolean equals(Object another) {
        if (this == another) return true;
        return another instanceof Delta
                && equalTo((Delta) another);
    }

    private boolean equalTo(Delta another) {
        return Objects.equals(role, another.role)
                && Objects.equals(content, another.content)
                && Objects.equals(toolCalls, another.toolCalls)
                && Objects.equals(functionCall, another.functionCall)
                && Objects.equals(reasoningContent, another.reasoningContent);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + Objects.hashCode(role);
        h += (h << 5) + Objects.hashCode(content);
        h += (h << 5) + Objects.hashCode(toolCalls);
        h += (h << 5) + Objects.hashCode(functionCall);
        h += (h << 5) + Objects.hashCode(reasoningContent);
        return h;
    }

    @Override
    public String toString() {
        return "Delta{"
                + "role=" + role
                + ", content=" + content
                + ", toolCalls=" + toolCalls
                + ", functionCall=" + functionCall
                + ", reasoningContent=" + reasoningContent
                + "}";
    }

    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static final class Builder {

        private String role;
        private String content;
        private List<ToolCall> toolCalls;
        @Deprecated
        private FunctionCall functionCall;
        private String reasoningContent;

        public Builder role(String role) {
            this.role = role;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder toolCalls(List<ToolCall> toolCalls) {
            this.toolCalls = toolCalls;
            return this;
        }

        @Deprecated
        public Builder functionCall(FunctionCall functionCall) {
            this.functionCall = functionCall;
            return this;
        }

        @JsonProperty("reasoning_content")
        public Builder reasoningContent(String reasoningContent) {
            this.reasoningContent = reasoningContent;
            return this;
        }

        public Delta build() {
            return new Delta(this);
        }
    }
}
