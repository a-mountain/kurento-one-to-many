package com.example.server.messages;

import com.example.server.messages.in.IceCandidateOffer;
import com.example.server.messages.in.PresenterOfferMessage;
import com.example.server.messages.in.ViewerOfferMessage;
import com.example.server.messages.bi.StopCommunication;
import com.example.server.messages.out.*;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = PresenterOfferMessage.class),
        @JsonSubTypes.Type(value = IceCandidateResponse.class),
        @JsonSubTypes.Type(value = AcceptedPresenterResponse.class),
        @JsonSubTypes.Type(value = RejectedPresenterResponse.class),
        @JsonSubTypes.Type(value = RejectedViewerResponse.class),
        @JsonSubTypes.Type(value = AcceptedViewerResponse.class),
        @JsonSubTypes.Type(value = ViewerOfferMessage.class),
        @JsonSubTypes.Type(value = IceCandidateOffer.class),
        @JsonSubTypes.Type(value = StopCommunication.class)
})
public interface Message { }
