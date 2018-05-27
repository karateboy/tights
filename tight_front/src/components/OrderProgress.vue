<template>
    <div>
        <table class="table table-hover table-bordered table-condensed">
            <thead>
            <tr>
                <th>顏色</th>
                <th>尺寸</th>
                <th>數量(打)</th>
                <th>完成</th>
                <th>進度(打): (已漂染/生產中/已完成)</th>
                <th>生產百分比</th>
                <th>損耗百分比(%)</th>
            </tr>
            </thead>
            <tbody>
            <tr v-for='(detail, idx) in order.details' :key="idx">
                <td>{{detail.color}}</td>
                <td>{{detail.size}}</td>
                <td>{{ showDozen(detail.quantity)}}</td>
                <td>
                    <i class="fa fa-check" aria-hidden="true" style="color:green" v-if='detail.complete'></i>
                    <i class="fa fa-times" style="color:red" aria-hidden="true" v-else></i>
                </td>
                <td>
                    <div v-if='productionSummary[idx]'>
                        <span class='text-info'>{{ showDozen(productionSummary[idx].dyed)}}</span>/
                        <span class='text-warning'>{{ showDozen(productionSummary[idx].inProduction)}}</span>/
                        <span class='text-success'>{{ showDozen(productionSummary[idx].produced)}}</span>
                    </div>
                </td>
                <td>
                    <div class="progress">
                        <div class="progress-bar progress-bar-success progress-bar-striped" role="progressbar"
                             :aria-valuenow="productionPercent(idx)" aria-valuemin="0" aria-valuemax="100" :style="{width:productionPercent(idx)+'%'}">
                            {{productionPercent(idx)}}%
                        </div>
                    </div>
                </td>
                <td>
                    <div class="progress">
                        <div class="progress-bar progress-bar-warning progress-bar-striped" role="progressbar"
                             :aria-valuenow="overheadPercent(idx)" aria-valuemin="0" aria-valuemax="100" :style="{width:overheadPercent(idx)+'%'}">
                            {{overheadPercent(idx)}}%
                        </div>
                    </div>
                </td>
            </tr>
            </tbody>
        </table>
    </div>
</template>
<style>
</style>
<script>
import { mapGetters } from "vuex";
import axios from "axios";
import * as dozenExpr from "../dozenExp";

export default {
  data() {
    return {
      productionSummary_: []
    };
  },
  computed: {
    ...mapGetters(["order"]),
    productionSummary() {
      this.productionSummary_.splice(0, this.productionSummary_.length);
      this.order.details.forEach((detail, idx) => {
        let summary = {
          dyed: 0,
          inProduction: 0,
          produced: 0,
          overhead: 0
        };

        this.productionSummary_.push(summary);
        axios
          .get("/OrderDetailProductionSummary/" + this.order._id + "/" + idx)
          .then(resp => {
            const ret = resp.data;
            summary.dyed = ret.dyed;
            summary.inProduction = ret.inProduction;
            summary.produced = ret.produced;
            summary.overhead = ret.overhead;
            summary.quantity = ret.quantity;
          })
          .catch(err => {
            alert(err);
          });
      });
      return this.productionSummary_;
    }
  },
  methods: {
    productionPercent(idx) {
      let production = this.productionSummary_[idx].dyed + this.productionSummary_[idx].inProduction + this.productionSummary_[idx].produced; 
      let percent =
        production /
        this.order.details[idx].quantity *
        100;
      return parseInt(percent);
    },
    overheadPercent(idx) {
      let percent =
        this.productionSummary_[idx].overhead /
        this.productionSummary_[idx].quantity *
        100;
      return parseInt(percent);
    },

    showDozen(v) {
      return dozenExpr.toDozenStr(v);
    }
  },
  components: {}
};
</script>
