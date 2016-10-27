package com.bjlx.QinShihuang.core;

import com.bjlx.QinShihuang.core.formatter.ValidationCodeFormatter;
import com.bjlx.QinShihuang.exception.BjlxException;
import com.bjlx.QinShihuang.model.account.PhoneNumber;
import com.bjlx.QinShihuang.model.account.UserInfo;
import com.bjlx.QinShihuang.model.misc.Sequence;
import com.bjlx.QinShihuang.model.misc.Token;
import com.bjlx.QinShihuang.model.misc.ValidationCode;
import com.bjlx.QinShihuang.utils.Constant;
import com.bjlx.QinShihuang.utils.ErrorCode;
import com.bjlx.QinShihuang.utils.MailerUtil;
import com.bjlx.QinShihuang.utils.MorphiaFactory;
import com.bjlx.QinShihuang.utils.SmsSendUtil;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

/**
 * 账户核心实现
 * Created by xiaozhi on 2016/10/21.
 */
public class AccountAPI {

	/**
	 * 取得数据库对象
	 */
	private static Datastore ds = MorphiaFactory.getInstance();
	
	/**
     * 取得下一个userId
     * @return 用户id
     */
    public long getNextUserId() {
        Query<Sequence> query = ds.createQuery(Sequence.class).field("column").equal(Sequence.fd_userId);
        UpdateOperations<Sequence> ops = ds.createUpdateOperations(Sequence.class).inc("count");
        Sequence ret = ds.findAndModify(query, ops, false, true);
        return ret.getCount();
    }
	
	
    /**
     * 初始化一个mapper对象
     */
    public static ObjectMapper mapper = new ObjectMapper().enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    
    /**
     * 是否允许发送验证码
     * @param account 账号
     * @param isTel 是否手机号
     * @return 是否允许
     */
    public static boolean isAllowSendValidationCode(String account, boolean isTel) {
    	long currentTime = System.currentTimeMillis();
    	
    	Query<ValidationCode> query = ds.createQuery(ValidationCode.class).field("action").equal(1);
    	if(isTel) {
    		query.field(ValidationCode.fd_number).equal(account).field(ValidationCode.fd_resendTime).greaterThan(currentTime);
    	} else {
    		query.field(ValidationCode.fd_email).equal(account).field(ValidationCode.fd_resendTime).greaterThan(currentTime);
    	}
    	return query.get() == null;
    }
    
    /**
     * 检验用户是否存在
     * @param account 用户账户
     * @param isTel 是否手机号
     * @return true表示存在，false表示不存在
     */
    private static boolean checkUserExist(String account, boolean isTel) {
    	Query<UserInfo> queryUser = ds.createQuery(UserInfo.class);
    	if(isTel) {
    		queryUser.field(UserInfo.fd_number).equal(account);
    	} else {
    		queryUser.field(UserInfo.fd_email).equal(account);
    	}
    	
    	return queryUser.get() != null;
    }

	/**
	 * 发送验证码
	 * @param account 用户账户
	 * @param action 验证码的用途
	 * @param isTel 是否手机号
	 * @return 发送结果
	 * @throws BjlxException 已知异常
	 * @throws Exception 运行时异常
	 */
    public static JsonNode sendValidationCode(String account, int action, boolean isTel) throws BjlxException, Exception {
    	
    	// 根据action检查异常情况
    	switch(action) {
	    	case Constant.NEW_USER_SIGNUP_ACTION :
	    		if(checkUserExist(account, isTel)) {
	    			throw new BjlxException(ErrorCode.USER_EXIST_1001);
	    		}
	    		break;
	    	case Constant.BIND_TEL_ACTION:
	    		if(checkUserExist(account, isTel)) {
	    			throw new BjlxException(ErrorCode.TEL_EXIST_1001);
	    		}
	    		break;
	    	case Constant.RESET_PWD_ACTION:
	    		if(!checkUserExist(account, isTel)) {
	    			throw new BjlxException(ErrorCode.USER_NOT_EXIST_1001);
	    		}
	    		break;
	    	case Constant.BIND_EMAIL_ACTION:
	    		if(checkUserExist(account, isTel)) {
	    			throw new BjlxException(ErrorCode.EMAIL_EXIST_1001);
	    		}
	    		break;
    	}
        // 产生验证码
        String code = String.format("%d", (int) (Math.random() * 1000000));
        // 短信内容
        String[] smsdata = new String[1];
        smsdata[0] = code;
        Long currentTime = System.currentTimeMillis();
        ValidationCode result = null;
        Query<ValidationCode> query = ds.createQuery(ValidationCode.class);
        UpdateOperations<ValidationCode> ops = ds.createUpdateOperations(ValidationCode.class);
        if(isTel) {
			PhoneNumber phoneNumber = new PhoneNumber(86, account);
            ValidationCode validationCode = new ValidationCode(currentTime, currentTime + 6 * 60 * 1000, code, phoneNumber, action);
        	// 存数据库
            query.field(ValidationCode.fd_number).equal(account);

            ops.set(ValidationCode.fd_tel, validationCode.getTel())
        	   	.set(ValidationCode.fd_createTime, validationCode.getCreateTime())
        		.set(ValidationCode.fd_expireTime, validationCode.getExpireTime())
        		.set(ValidationCode.fd_action, validationCode.getAction())
        		.set(ValidationCode.fd_code, validationCode.getCode())
        		.set(ValidationCode.fd_lastSendTime, validationCode.getLastSendTime())
        		.set(ValidationCode.fd_used, validationCode.isUsed())
        		.set(ValidationCode.fd_resendTime, validationCode.getResendTime())
        		.set(ValidationCode.fd_failCnt, validationCode.getFailCnt());
            try {
            	result = ds.findAndModify(query, ops, false, true);
            } catch(Exception e) {
				e.printStackTrace();
            	throw e;
            }
	        // 发送短信
	        int smsResult = SmsSendUtil.sendMessageByTemplate(account, smsdata);
	        switch(smsResult) {
				case -1: throw new BjlxException(ErrorCode.NETWORK_ERROR);
				case 1:  break;// 成功
				case 2: throw new BjlxException(ErrorCode.TIME_LIMIT_1001);
				case 3:
				case 4: 
				case 5:
					throw new BjlxException(ErrorCode.REQUEST_TOO_MANY_1001); 
				default:
					throw new BjlxException(ErrorCode.UNKNOWN);  
			}
        } else {
			// 过期时间设置为6分钟
			Long expireTime = 6 * 60 * 1000L;

        	ValidationCode validationCode = new ValidationCode(currentTime, currentTime + expireTime, code, account, action);
        	// 存数据库
        	query.field(ValidationCode.fd_email).equal(account);
        	ops.set(ValidationCode.fd_email, validationCode.getEmail())
        		.set(ValidationCode.fd_createTime, validationCode.getCreateTime())
        		.set(ValidationCode.fd_expireTime, validationCode.getExpireTime())
        		.set(ValidationCode.fd_action, validationCode.getAction())
        		.set(ValidationCode.fd_code, validationCode.getCode())
        		.set(ValidationCode.fd_lastSendTime, validationCode.getLastSendTime())
        		.set(ValidationCode.fd_used, validationCode.isUsed())
        		.set(ValidationCode.fd_resendTime, validationCode.getResendTime())
        		.set(ValidationCode.fd_failCnt, validationCode.getFailCnt());
        	try {
            	result = ds.findAndModify(query, ops, false, true);
    	     	MailerUtil.sendSimpleEmail("不羁旅行验证码", String.format("您的验证码为:%s", code), "service@bujilvxing.com", account, "");
            } catch(Exception e) {
				e.printStackTrace();
            	throw e;
            }
        }
        ObjectMapper mapper = ValidationCodeFormatter.getMapper();
        return mapper.valueToTree(result);
    }

	/**
	 * 是否验证码，验证码必须是6位的数字
	 * @param code 验证码
	 * @return 是否验证码
	 * @throws PatternSyntaxException 模式匹配异常
	 */
	public static boolean isCode(String code) throws PatternSyntaxException {
		String regExp = "^\\d{6}$";
		Pattern p = Pattern.compile(regExp);
		Matcher m = p.matcher(code);
		return m.matches();
	}

	/**
	 * 检验验证码的合法性，并返回合法的令牌
	 * @param account 账户
	 * @param code 验证码
	 * @param isTel 账户是否手机号
	 * @return 令牌
	 * @throws BjlxException 已知异常
	 * @throws Exception 运行时异常
	 */
    public static JsonNode checkValidationCode(String account, String code, boolean isTel) throws BjlxException, Exception {
		Query<ValidationCode> query = ds.createQuery(ValidationCode.class);

		long currentTime = System.currentTimeMillis();
		/**
		 * 符合条件的验证码
		 * 1、没有过期
		 * 2、没有被使用过
		 * 3、验证错误次数没有超过10次
		 * 4、账户和验证码正确
		 */
		if(isTel) {
			query.field(ValidationCode.fd_number).equal(account);
		} else {
			query.field(ValidationCode.fd_email).equal(account);
		}
		query.field(ValidationCode.fd_expireTime).greaterThanOrEq(currentTime).field(ValidationCode.fd_used).equal(false)
				.field(ValidationCode.fd_code).equal(code).field(ValidationCode.fd_failCnt).lessThanOrEq(10);

		try {
			if (query.get() == null) {
				throw new BjlxException(ErrorCode.VALIDATION_FAIL_1002);
			} else {
				// 存入token并返回
				Token token = new Token();
				ds.save(token);

				// 将验证码设置为已使用
				UpdateOperations<ValidationCode> ops = ds.createUpdateOperations(ValidationCode.class).set(ValidationCode.fd_used, true);
				ds.updateFirst(query, ops);

				ObjectNode result = mapper.createObjectNode();
				// 检验完后，将使用设置为true
				result.put("token", token.getToken());
				return result;
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
    }
}