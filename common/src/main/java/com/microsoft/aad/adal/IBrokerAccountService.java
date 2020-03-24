/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: C:\\projects\\github.com\\azuread\\android-complete\\common\\common\\src\\main\\aidl\\com\\microsoft\\aad\\adal\\IBrokerAccountService.txt
 */
package com.microsoft.aad.adal;
/**
 * Broker Account service APIs provided by the broker app. Those APIs will be responsible for interacting with the
 * account manager API. Calling app does not need to request for contacts permission if the broker installed on the
 * device has the support for the bound service.
 */
@SuppressWarnings({"rawtypes", "cast"})
public interface IBrokerAccountService extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements IBrokerAccountService
{
private static final String DESCRIPTOR = "com.microsoft.aad.adal.IBrokerAccountService";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.microsoft.aad.adal.IBrokerAccountService interface,
 * generating a proxy if needed.
 */
public static IBrokerAccountService asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof IBrokerAccountService))) {
return ((IBrokerAccountService)iin);
}
return new IBrokerAccountService.Stub.Proxy(obj);
}
@Override public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
String descriptor = DESCRIPTOR;
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(descriptor);
return true;
}
case TRANSACTION_getBrokerUsers:
{
data.enforceInterface(descriptor);
android.os.Bundle _result = this.getBrokerUsers();
reply.writeNoException();
if ((_result!=null)) {
reply.writeInt(1);
_result.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
}
else {
reply.writeInt(0);
}
return true;
}
case TRANSACTION_acquireTokenSilently:
{
data.enforceInterface(descriptor);
java.util.Map _arg0;
ClassLoader cl = (ClassLoader)this.getClass().getClassLoader();
_arg0 = data.readHashMap(cl);
android.os.Bundle _result = this.acquireTokenSilently(_arg0);
reply.writeNoException();
if ((_result!=null)) {
reply.writeInt(1);
_result.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
}
else {
reply.writeInt(0);
}
return true;
}
case TRANSACTION_getIntentForInteractiveRequest:
{
data.enforceInterface(descriptor);
android.content.Intent _result = this.getIntentForInteractiveRequest();
reply.writeNoException();
if ((_result!=null)) {
reply.writeInt(1);
_result.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
}
else {
reply.writeInt(0);
}
return true;
}
case TRANSACTION_removeAccounts:
{
data.enforceInterface(descriptor);
this.removeAccounts();
reply.writeNoException();
return true;
}
case TRANSACTION_getInactiveBrokerKey:
{
data.enforceInterface(descriptor);
android.os.Bundle _arg0;
if ((0!=data.readInt())) {
_arg0 = android.os.Bundle.CREATOR.createFromParcel(data);
}
else {
_arg0 = null;
}
android.os.Bundle _result = this.getInactiveBrokerKey(_arg0);
reply.writeNoException();
if ((_result!=null)) {
reply.writeInt(1);
_result.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
}
else {
reply.writeInt(0);
}
return true;
}
default:
{
return super.onTransact(code, data, reply, flags);
}
}
}
private static class Proxy implements IBrokerAccountService
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
@Override public android.os.IBinder asBinder()
{
return mRemote;
}
public String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
@Override public android.os.Bundle getBrokerUsers() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
android.os.Bundle _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getBrokerUsers, _data, _reply, 0);
_reply.readException();
if ((0!=_reply.readInt())) {
_result = android.os.Bundle.CREATOR.createFromParcel(_reply);
}
else {
_result = null;
}
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public android.os.Bundle acquireTokenSilently(java.util.Map requestParameters) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
android.os.Bundle _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeMap(requestParameters);
mRemote.transact(Stub.TRANSACTION_acquireTokenSilently, _data, _reply, 0);
_reply.readException();
if ((0!=_reply.readInt())) {
_result = android.os.Bundle.CREATOR.createFromParcel(_reply);
}
else {
_result = null;
}
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public android.content.Intent getIntentForInteractiveRequest() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
android.content.Intent _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getIntentForInteractiveRequest, _data, _reply, 0);
_reply.readException();
if ((0!=_reply.readInt())) {
_result = android.content.Intent.CREATOR.createFromParcel(_reply);
}
else {
_result = null;
}
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public void removeAccounts() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_removeAccounts, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public android.os.Bundle getInactiveBrokerKey(android.os.Bundle bundle) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
android.os.Bundle _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
if ((bundle!=null)) {
_data.writeInt(1);
bundle.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
mRemote.transact(Stub.TRANSACTION_getInactiveBrokerKey, _data, _reply, 0);
_reply.readException();
if ((0!=_reply.readInt())) {
_result = android.os.Bundle.CREATOR.createFromParcel(_reply);
}
else {
_result = null;
}
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
}
static final int TRANSACTION_getBrokerUsers = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_acquireTokenSilently = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_getIntentForInteractiveRequest = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_removeAccounts = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_getInactiveBrokerKey = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
}
public android.os.Bundle getBrokerUsers() throws android.os.RemoteException;
public android.os.Bundle acquireTokenSilently(java.util.Map requestParameters) throws android.os.RemoteException;
public android.content.Intent getIntentForInteractiveRequest() throws android.os.RemoteException;
public void removeAccounts() throws android.os.RemoteException;
public android.os.Bundle getInactiveBrokerKey(android.os.Bundle bundle) throws android.os.RemoteException;
}
