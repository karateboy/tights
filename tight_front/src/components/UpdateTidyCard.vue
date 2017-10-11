<template>
    <div class="ibox-content">
        <form class="form-horizontal" @submit.prevent="query">
            <div class="form-group has-feedback"><label class="col-lg-3 control-label">流動工作卡號:</label>
                <div class="col-lg-5"><input type="text" placeholder="掃描條碼" autofocus
                                             class="form-control"
                                             v-model="workCardID">
                </div>
            </div>
        </form>
        <div v-if='displayCard'>
            <tidy-card :tidyCard='tidyCard' v-on:updated='cleanup' :quantity='quantity'></tidy-card>
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
    import TidyCard from './TidyCard.vue'

    export default{
        data(){
            return {
                workCardID: "",
                displayCard: false,
                quantity:0,
                tidyCard: {}
            }
        },
        methods: {
            query(){
                const phase = this.$route.params.phase
                axios.post("/GetTidyCard", {workCardID: this.workCardID, phase}).then((resp) => {
                    const ret = resp.data
                    this.quantity = ret.quantity
                    this.tidyCard = ret.card
                    this.displayCard = true
                }).catch((error) => {
                    if (error.response) {
                        alert(error.response.data);
                    } else {
                        alert(error.message);
                    }
                })
            },
            cleanup(){
                this.displayCard = false
                this.workCardID = ""
            }
        },
        components: {
            TidyCard
        }
    }
</script>
