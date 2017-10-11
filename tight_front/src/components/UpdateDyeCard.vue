<template>
    <div>
        <form class="form-horizontal" @submit.prevent="query">
            <div class="form-group has-feedback"><label class="col-lg-3 control-label">漂染單編號:</label>
                <div class="col-lg-5"><input type="text" placeholder="掃描條碼" autofocus
                                             class="form-control"
                                             v-model="id">
                    <span v-if="invalidId" class="glyphicon glyphicon-remove form-control-feedback"></span>
                    <span v-if="invalidId" class="help-block">無效的漂染包襪明細單號</span>
                </div>
            </div>
        </form>
        <div v-if='displayCard'>
            <dye-card-detail  :edit="true" :dyeCard='dyeCard' @updated='cleanup'></dye-card-detail>
        </div>
    </div>
</template>
<style scoped>
    body{
        background-color:#ff0000;
    }


</style>
<script>
    import DyeCardDetail from './DyeCardDetail.vue'
    import axios from 'axios'

    export default{
        data(){
            return {
                id: "",
                invalidId: false,
                displayCard: false,
                dyeCard: {}
            }
        },
        methods: {
            query(){
                axios.get("/DyeCard/"+ this.id).then((resp)=>{
                    const ret = resp.data
                    if(resp.status == 200){
                        this.dyeCard = ret
                        this.displayCard = true
                    }else{
                        alert("找不到漂染卡紀錄:" + resp.statusText)
                        this.id = ""
                    }
                }).catch((err)=>{
                    alert(err)
                })

            },
            cleanup(){
                this.id = ""
                this.displayCard = false
            }
        },
        components: {
            DyeCardDetail
        }
    }
</script>
