<template>
    <div>
        <form class="form-horizontal" @submit.prevent="query">
            <div class="form-group has-feedback"><label class="col-lg-3 control-label">流動工作卡號:</label>
                <div class="col-lg-5"><input type="text" placeholder="掃描條碼" autofocus
                                             class="form-control"
                                             v-model="workCardID">
                </div>
            </div>
        </form>
        <br>
        <div v-if='displayCard'>
            <styling-card :stylingCard='stylingCard' :workCardID='workCardID' :quantity='quantity' v-on:updated='cleanup'></styling-card>
        </div>
    </div>
</template>
<style scoped>
    body{
        background-color:#ff0000;
    }



</style>
<script>
    import axios from 'axios'
    import StylingCard from './StylingCard.vue'

    export default{
        data(){
            return {
                workCardID: "",
                displayCard: false,
                stylingCard: {
                    date: 0
                },
                quantity: 0
            }
        },
        methods: {
            query(){
                axios.get("/WorkCard/" + this.workCardID).then((resp) => {
                    const ret = resp.data
                    if (resp.status == 200) {
                        let workCard = ret
                        if (workCard.stylingCard == null)
                            this.stylingCard = {
                                operator:[],
                                date: 0
                            }
                        else
                            this.stylingCard = workCard.stylingCard
                        this.displayCard = true
                        this.quantity = workCard.quantity
                    } else {
                        alert("找不到流動工作卡:")
                        this.workCardID = ""
                    }
                }).catch((err) => {
                    alert(err)
                })
            },
            cleanup(){
                this.displayCard = false
                this.workCardID = ""
            }
        },
        components: {
            StylingCard
        }
    }
</script>
