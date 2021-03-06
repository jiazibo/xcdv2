package com.bms.order.rest;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.bms.order.archive.impl.Operator;
import com.bms.order.bean.param.ORDR0102IBean;
import com.bms.order.common.rest.BusinessRestParam;
import com.bms.order.common.rest.BusinessRestResult;
import com.bms.order.common.rest.result.RestFailedData;
import com.bms.order.services.ORDR0102IService;
import com.bms.order.validator.ORDR0102IRestValidator;
import com.framework.boot.base.BaseRestController;
import com.framework.exception.BusinessException;
import com.framework.exception.bean.BusinessExceptionMessage;
import com.framework.i18n.util.MessageManager;
import com.framework.validator.annotation.Validator;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 买家订单修改接口Controller
 *
 * @author li_huiqian
 */
@RestController
@Api(tags = { "ORDR0102IRestController" }, description = "买家订单修改接口")
public class ORDR0102IRestController extends BaseRestController {

    @Autowired
    private ORDR0102IService service;

    /**
     * 订单配送通知单发送给美迪福之前，对订单基本信息进行修改
     *
     * @param param
     *            BusinessRestParam
     * @return BusinessRestResult
     */
    @ApiOperation(value = "买家订单修改接口", notes = "订单配送通知单发送给美迪福之前，对订单基本信息进行修改")
    @Validator(validatorClass = ORDR0102IRestValidator.class)
    @RequestMapping(value = "/order/by/_modify", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public BusinessRestResult execute(@RequestBody BusinessRestParam<ORDR0102IBean> param) {
        List<ORDR0102IBean> beans = param.getBeans();
        Operator operator = Operator.Builder.build(param.getOperator());
        BusinessRestResult result = new BusinessRestResult();

        int processedCount = 0; // 处理条数
        int succeedCount = 0; // 处理成功条数
        int failedCount = 0; // 处理失败条数

        for (int i = 0; i < beans.size(); i++) {
            ORDR0102IBean bean = beans.get(i);
            try {
                // step1 与 step2 是不同的事务
                // step1 删除预配送明细（如果有的话）
                // step2 修改买家订单

                // 删除预配送明细
                service.execute(operator, bean, /* step1 */1);

                // 修改订单
                service.execute(operator, bean, /* step2 */2);

                succeedCount++;
            } catch (BusinessException e) {
                e.printStackTrace();

                BusinessExceptionMessage bem = e.getBusinessExceptionMessages().get(0); // 没有多message的Exception
                RestFailedData failedData = new RestFailedData();
                failedData.setFailedDataId(bean.getBuyerOrderId());
                failedData.setIndex(i);
                failedData.setErrorParams(bem.getMessageParams());
                failedData.setModuleName(bem.getModuleName());
                failedData.setErrorCode(bem.getMessageCode());
                failedData.setErrorMessage(MessageManager.getMessage(bem.getMessageCode(), bem.getMessageParams()));
                result.putFailedData(failedData);

                failedCount++;
            } finally {
                processedCount++;
            }
        }

        result.setProcessedCount(processedCount);
        result.setSucceedCount(succeedCount);
        result.setFailedCount(failedCount);
        return result;
    }
}
