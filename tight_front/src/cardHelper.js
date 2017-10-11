/**
 * Created by user on 2017/1/22.
 */
import axios from 'axios'
export default {
    populateWorkCard(workCard){
        workCard.order = {}
        axios.get("/Order/"+workCard.orderId).then((resp)=>{
            workCard.order = resp.data
        })
    },
    populateTidyCard(tidyCard){
        tidyCard.workCard = {}
        axios.post("/GetWorkCards", [tidyCard.workCardID]).then((resp)=>{
            const ret = resp.data
            for(let workCard of ret){
                this.populateWorkCard(workCard)
                tidyCard.workCard = workCard
            }
        })

    },
    populateDyeCard(dyeCard){
        dyeCard.workCards = [];
        axios.post("/GetWorkCards", dyeCard.workIdList).then((resp)=>{
            const ret = resp.data
            dyeCard.workCards.splice(0, dyeCard.workCards.length)
            for(let workCard of ret){
                this.populateWorkCard(workCard)
                dyeCard.workCards.push(workCard)
            }
        })
    },
    getOrderProductionSummary(order){
        order.productionSummary = []
        axios.get("/OrderProductionSummary/" + order._id).then((resp)=>{
            order.productionSummary.splice(0, order.productionSummary.length)
            order.productionSummary.push(resp.data)
        })
    }
}
