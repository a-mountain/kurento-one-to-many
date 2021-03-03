import {sendMessage, setHandlers, initSocket} from "./socket.js";

//bi
const IceCandidateMessage = 'IceCandidateMessage';
const StopCommunicationMessage = 'StopCommunicationMessage';
// in
const StartStreamMessage = 'StartStreamMessage';
const StartWatchMessage = 'StartWatchMessage';
// out
const SdpAnswerMessage = 'SdpAnswerMessage';
const RejectionMessage = 'RejectionMessage';

const video = document.querySelector('#stream');
const btnStartWatch = document.querySelector('#watch');
const btnStartBroadcast = document.querySelector('#broadcast');
const btnStop = document.querySelector('#stop');

initSocket();

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
            sendMessage(IceCandidateMessage, {
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

const startStream = async () => {
    peerConnection = createPeerConnection()
    await startVideo();
    localStream.getTracks().forEach(track => peerConnection.addTrack(track));
    const offer = await peerConnection.createOffer()
    await peerConnection.setLocalDescription(offer)
    sendMessage(StartStreamMessage, {
        sdpOffer: offer.sdp
    })
}

const startWatch = async () => {
    peerConnection = createPeerConnection()
    const offer = await peerConnection.createOffer({offerToReceiveAudio: false, offerToReceiveVideo: true})
    await peerConnection.setLocalDescription(offer)
    sendMessage(StartWatchMessage, {
        sdpOffer: offer.sdp
    })
}

const stopStream = stream => {
    stream.getTracks().forEach(track => track.stop());
}

const handleSdpAnswer = async data => {
    const {sdpAnswer} = data;
    const sdp = {type: 'answer', sdp: sdpAnswer}
    await peerConnection.setRemoteDescription(sdp)
    console.info('Set remote description');
}

const handleIceCandidate = async data => {
    const {candidate} = data;
    const iceCandidate = new RTCIceCandidate(candidate);
    await peerConnection.addIceCandidate(iceCandidate);
    console.info('Add ice candidate');
}

const handleRejection = async data => {
    const {reason} = data;
    alert(reason);
}

// const handleStopCommunication = async () => {
//     console.log('Stop communication');
//     try {
//         if (peerConnection && peerConnection.signalingState !== 'closed') {
//             if (localStream) {
//                 localStream.getTracks().forEach(track => peerConnection.removeTrack(track));
//             }
//             peerConnection.close();
//         }
//         if (localStream) {
//             localStream.getTracks().forEach(track => localStream.removeTrack(track));
//         }
//         localStream = null;
//         video.srcObject = null;
//         video.stop();
//     } catch (e) {
//         console.error("Stop ")
//     }
// }

const handlers = {
    [SdpAnswerMessage]: handleSdpAnswer,
    [IceCandidateMessage]: handleIceCandidate,
    [RejectionMessage]: handleRejection,
    // [StopCommunicationMessage]: handleStopCommunication,
};

setHandlers(handlers);

btnStartBroadcast.addEventListener('click', startStream)
btnStartWatch.addEventListener('click', startWatch)
