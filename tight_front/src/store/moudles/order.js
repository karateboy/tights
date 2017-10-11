/**
 * Created by user on 2017/1/20.
 */
const emptyOrder = {
    _id: "",
    salesId: "",
    name: "",
    expectedDeliverDate: new Date().getTime(),
    factoryId: "",
    customerId: "",
    brand: "",
    date: 0,
    details: [],
    notices: [],
    packageInfo: {
        packageOption: [false, false, false, false, false],
        packageNote: "",
        labelOption: [false, false, false, false, true],
        labelNote: "",
        cardOption: [false, false, false, false],
        cardNote: ['','','',''],
        bagOption: [false, false, false, false, false, false],
        pvcNote: "",
        numInBag: 3,
        bagNote: "",
        exportBoxOption: [true, true],
        exportBoxNote: ['',''],
        ShippingMark: "",
        extraNote: ""
    },
    active: true
}

const state = {
    isNew:true,
    order: JSON.parse(JSON.stringify( emptyOrder))
}

const getters = {
    order: state =>{
        return state.order;
    },
    isNewOrder: state =>{
        return state.isNew
    }
}

const mutations = {
    updateOrder: (state, payload) => {
        state.order = payload.order
        state.isNew = payload.isNew
    }
}

const actions = {
    newOrder : ({commit}) => {
        const newOrder = JSON.parse(JSON.stringify( emptyOrder))
        commit('updateOrder', {order:newOrder, isNew:true});
    },
    showOrder : ({commit}, myOrder) =>{
        commit('updateOrder', {order:myOrder, isNew:false})
    },
    cloneOrder : ({commit}, myOrder) =>{
        const cloneOrder = JSON.parse(JSON.stringify(myOrder))
        cloneOrder._id = ""
        for(let detail of cloneOrder.details){
            detail.workCardIDs = []
            detail.finishedWorkCards = []
            detail.complete = false
        }
        cloneOrder.expectedDeliverDate = new Date().getTime()
        cloneOrder.active = true
        commit('updateOrder', {order:cloneOrder, isNew:true})
    }
}

export default {
    state,
    getters,
    mutations,
    actions
}
