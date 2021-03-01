const CALL_ENDPOINT = 'ws://localhost:8443/call';
const ws = new WebSocket(CALL_ENDPOINT);
const OPEN = 1;

// in
const IceCandidateOffer = 'IceCandidateOffer';
const PresenterOfferMessage = 'PresenterOfferMessage';
const ViewerOfferMessage = 'ViewerOfferMessage';
// out
const AcceptedPresenterResponse = 'AcceptedPresenterResponse';
const AcceptedViewerResponse = 'AcceptedViewerResponse';
const IceCandidateResponse = 'IceCandidateResponse';

const video = document.querySelector('#stream');
const btnStartWatch = document.querySelector('#watch');
const btnStartBroadcast = document.querySelector('#broadcast');
const btnStop = document.querySelector('#stop');

const displayMediaConstraints = {
    video: {
        width: 854,
        height: 480,
        frameRate: 30
    },
    audio: false
}

let localStream;
let peerConnection;


const sendMessage = (type, content) => {
    if (ws.readyState === OPEN) {
        const message = {type, ...content};
        const jsonMessage = JSON.stringify(message)
        ws.send(jsonMessage);
        console.info(`Send message: ${jsonMessage}`)
    }
};

const createPeerConnection = () => {
    const configuration = {
        'iceServers': [
            {'urls': 'stun:stun.stunprotocol.org:3478'},
            {'urls': 'stun:stun.l.google.com:19302'},
        ]
    };
    const peerConnection = new RTCPeerConnection(configuration);
    console.info("Peer connection is created");
    peerConnection.onicecandidate = async event => {
        const iceCandidate = event.candidate;
        if (iceCandidate) {
            sendMessage(IceCandidateOffer, {
                candidate: iceCandidate
            })
            return;
        }
        console.info(`Icecandidate even: ${event}`)
    };
    peerConnection.ontrack = event => {
        console.info(`Add track event : ${event}`);
       if (!localStream) {
           console.info(`Add track : ${event}`);
           video.srcObject = event.streams[0]
           video.play()
       }
    };
    peerConnection.onsignalingstatechange = event => {
        console.info(`Signaling state changed: ${JSON.stringify(event)}`)
    }
    peerConnection.addEventListener('connectionstatechange', event => {
        if (peerConnection.connectionState === 'connected') {
            console.info('Connection established')
        }
    })
    return peerConnection;
}

const startVideo = async () => {
    const stream = await navigator.mediaDevices.getDisplayMedia(displayMediaConstraints)
    console.info('Start Display media stream');
    localStream = stream;
    video.srcObject = stream;
    video.play();
    console.info('Play video');
}

const startBroadcast = async () => {
    peerConnection = createPeerConnection()
    await startVideo();
    localStream.getTracks().forEach(track => peerConnection.addTrack(track));
    const offer = await peerConnection.createOffer()
    await peerConnection.setLocalDescription(offer)
    sendMessage(PresenterOfferMessage, {
        sdpOffer: offer.sdp
    })
}

const startWatch = async () => {
    peerConnection = createPeerConnection()
    const offer = await peerConnection.createOffer({ offerToReceiveAudio: false, offerToReceiveVideo: true })
    await peerConnection.setLocalDescription(offer)
    sendMessage(ViewerOfferMessage, {
        sdpOffer: offer.sdp
    })
}

const setOnMessage = (onMessage) => {
    ws.onmessage = event => {
        const parsedData = JSON.parse(event.data)
        console.info(`WebSocket message received ${JSON.stringify(parsedData)}`)
        onMessage(parsedData);
    };
};

const onMessage = async data => {
    const type = data.type;
    if (type === AcceptedViewerResponse || type === AcceptedPresenterResponse) {
        const {sdpAnswer} = data;
        const sdp = {type: 'answer', sdp: sdpAnswer}
        await peerConnection.setRemoteDescription(sdp)
        console.info('Set remote description');
        return;
    }
    if (type === IceCandidateResponse) {
        const {candidate} = data;
        const iceCandidate = new RTCIceCandidate(candidate);
        await peerConnection.addIceCandidate(iceCandidate);
        console.info('Add ice candidate');
        return;
    }
    console.error(`Unrecognized message type: ${JSON.stringify(data)}`)
};

ws.onerror = (event) => {
    console.error(`WebSocket error observed: ${event}`);
};

ws.onopen = () => {
    console.info(`WebSocket is open: ${CALL_ENDPOINT}`);
};

ws.onclose = () => {
    console.info('WebSocket is closed');
};

btnStartBroadcast.addEventListener('click', startBroadcast)
btnStartWatch.addEventListener('click', startWatch)

setOnMessage(onMessage);
