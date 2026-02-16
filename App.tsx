import React, { useState, useEffect } from 'react';
import { AppScreen, AssociationData, Member, MonthRecord } from './types';
import { Trash2, Users, Calendar, Plus, ArrowRight, Wallet, CheckCircle, RefreshCw, Smartphone } from 'lucide-react';
import { Button } from './components/Button';
import { Input } from './components/Input';
import { formatCurrency, getMonthName, generateId } from './utils/helpers';

// Storage Key
const STORAGE_KEY = 'jamiya_app_data_v1';

const App = () => {
  // State
  const [screen, setScreen] = useState<AppScreen>(AppScreen.SETUP);
  const [data, setData] = useState<AssociationData | null>(null);

  // Loading from LocalStorage
  useEffect(() => {
    const savedData = localStorage.getItem(STORAGE_KEY);
    if (savedData) {
      try {
        setData(JSON.parse(savedData));
        setScreen(AppScreen.DASHBOARD);
      } catch (e) {
        console.error("Failed to parse saved data");
      }
    }
  }, []);

  // Saving to LocalStorage
  useEffect(() => {
    if (data) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
    }
  }, [data]);

  // --- Actions ---

  const handleCreateAssociation = (name: string, amount: number, startDate: string, members: string[]) => {
    const memberObjects: Member[] = members.map((m, index) => ({
      id: generateId(),
      name: m,
      order: index
    }));

    const newData: AssociationData = {
      id: generateId(),
      name,
      amountPerPerson: amount,
      startDate,
      members: memberObjects,
      currentMonthIndex: 0,
      history: {} // Start empty
    };

    setData(newData);
    setScreen(AppScreen.DASHBOARD);
  };

  const handleReset = () => {
    if (confirm('هل أنت متأكد من حذف الجمعية والبدء من جديد؟ لا يمكن التراجع عن هذا الإجراء.')) {
      localStorage.removeItem(STORAGE_KEY);
      setData(null);
      setScreen(AppScreen.SETUP);
    }
  };

  // --- Views ---

  if (screen === AppScreen.SETUP) {
    return <SetupView onCreate={handleCreateAssociation} />;
  }

  if (screen === AppScreen.DASHBOARD && data) {
    return <DashboardView data={data} onUpdate={setData} onReset={handleReset} />;
  }

  return null;
};

// --- Sub-Components ---

/**
 * Screen 1: Setup View (Android Style Form)
 */
const SetupView: React.FC<{ onCreate: (name: string, amount: number, date: string, members: string[]) => void }> = ({ onCreate }) => {
  const [name, setName] = useState('');
  const [amount, setAmount] = useState<string>('');
  const [date, setDate] = useState(new Date().toISOString().slice(0, 7)); // YYYY-MM
  const [memberName, setMemberName] = useState('');
  const [members, setMembers] = useState<string[]>([]);

  const addMember = () => {
    if (memberName.trim()) {
      setMembers([...members, memberName.trim()]);
      setMemberName('');
    }
  };

  const removeMember = (index: number) => {
    setMembers(members.filter((_, i) => i !== index));
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name || !amount || members.length < 2) {
      alert("الرجاء إكمال جميع البيانات وإضافة عضوين على الأقل");
      return;
    }
    onCreate(name, parseFloat(amount), date, members);
  };

  return (
    <div className="min-h-screen bg-white flex flex-col">
      {/* App Bar */}
      <div className="bg-primary text-white p-4 pt-8 shadow-md">
        <h1 className="text-xl font-bold text-center">تطبيق الجمعية</h1>
      </div>

      <div className="flex-1 overflow-y-auto p-4 pb-24">
        <div className="text-center space-y-2 mb-6 mt-4">
          <div className="bg-emerald-50 w-16 h-16 rounded-full flex items-center justify-center mx-auto text-primary mb-2 shadow-inner">
            <Users size={32} />
          </div>
          <h2 className="text-lg font-bold text-gray-800">إعدادات الجمعية</h2>
          <p className="text-gray-500 text-xs">قم بإدخال البيانات لإنشاء دورة جديدة</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="bg-white p-4 rounded-xl border border-gray-100 shadow-sm space-y-4">
            <Input 
              label="اسم الجمعية" 
              placeholder="مثال: جمعية العائلة" 
              value={name} 
              onChange={(e) => setName(e.target.value)} 
            />
            
            <div className="grid grid-cols-2 gap-4">
              <Input 
                label="القسط (ج.م)" 
                type="number" 
                placeholder="1000" 
                value={amount} 
                onChange={(e) => setAmount(e.target.value)} 
              />
              <Input 
                label="بداية الجمعية" 
                type="month" 
                value={date} 
                onChange={(e) => setDate(e.target.value)} 
              />
            </div>
          </div>

          <div className="bg-white p-4 rounded-xl border border-gray-100 shadow-sm">
            <label className="text-sm font-semibold text-gray-700 block mb-2">الأعضاء (ترتيب القبض)</label>
            <div className="flex gap-2 mb-4">
              <Input 
                placeholder="اسم العضو" 
                value={memberName} 
                onChange={(e) => setMemberName(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && (e.preventDefault(), addMember())}
              />
              <Button type="button" onClick={addMember} className="aspect-square p-0 w-[50px] bg-secondary">
                <Plus size={24} />
              </Button>
            </div>

            <div className="space-y-2">
              {members.length === 0 && <div className="text-center p-4 bg-gray-50 rounded-lg text-gray-400 text-sm border border-dashed border-gray-300">أضف الأعضاء بالترتيب</div>}
              {members.map((m, idx) => (
                <div key={idx} className="flex items-center justify-between bg-gray-50 p-3 rounded-lg border border-gray-100 animate-in fade-in slide-in-from-bottom-1">
                  <div className="flex items-center gap-3">
                    <span className="bg-white border border-gray-200 text-gray-600 w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold shadow-sm">
                      {idx + 1}
                    </span>
                    <span className="font-medium text-gray-700">{m}</span>
                  </div>
                  <button type="button" onClick={() => removeMember(idx)} className="text-red-400 p-1 hover:bg-red-50 rounded-full">
                    <Trash2 size={16} />
                  </button>
                </div>
              ))}
            </div>
          </div>
        </form>
      </div>

      {/* Floating Action Button Area */}
      <div className="fixed bottom-0 left-0 right-0 p-4 bg-white border-t border-gray-100">
        <Button onClick={handleSubmit} fullWidth disabled={members.length < 2} className="shadow-lg">
          حفظ وبدء الجمعية
        </Button>
      </div>
    </div>
  );
};

/**
 * Screen 2: Dashboard View
 */
const DashboardView: React.FC<{ data: AssociationData; onUpdate: (d: AssociationData) => void; onReset: () => void }> = ({ data, onUpdate, onReset }) => {
  const currentIndex = data.currentMonthIndex;
  
  const currentHistory: MonthRecord = data.history[currentIndex] || {
    monthIndex: currentIndex,
    payments: {},
    payoutCompleted: false
  };

  const recipientMember = data.members[currentIndex % data.members.length];
  const isFinished = currentIndex >= data.members.length;

  const totalExpected = data.amountPerPerson * data.members.length;
  const paidCount = Object.values(currentHistory.payments).filter(Boolean).length;
  const totalCollected = paidCount * data.amountPerPerson;
  const remaining = totalExpected - totalCollected;
  const isCollectionComplete = remaining === 0;

  const togglePayment = (memberId: string) => {
    if (currentHistory.payoutCompleted) return;

    const newPayments = { ...currentHistory.payments, [memberId]: !currentHistory.payments[memberId] };
    const newHistory = {
      ...data.history,
      [currentIndex]: {
        ...currentHistory,
        payments: newPayments
      }
    };
    onUpdate({ ...data, history: newHistory });
  };

  const handlePayoutToggle = () => {
    if (!isCollectionComplete) return;

    const newHistory = {
      ...data.history,
      [currentIndex]: {
        ...currentHistory,
        payoutCompleted: !currentHistory.payoutCompleted
      }
    };
    onUpdate({ ...data, history: newHistory });
  };

  const moveToNextMonth = () => {
    if (!currentHistory.payoutCompleted) return;
    if (currentIndex + 1 >= data.members.length) {
       alert("تم اكتمال الجمعية بالكامل!");
       return;
    }
    onUpdate({
      ...data,
      currentMonthIndex: currentIndex + 1
    });
  };

  if (isFinished) {
    return (
      <div className="min-h-screen bg-white flex flex-col items-center justify-center p-6 text-center">
        <div className="bg-green-100 w-24 h-24 rounded-full flex items-center justify-center text-green-600 mb-6 shadow-inner animate-in zoom-in">
          <CheckCircle size={48} />
        </div>
        <h2 className="text-3xl font-bold text-gray-800 mb-2">اكتملت الجمعية!</h2>
        <p className="text-gray-500 mb-8">تم تسليم جميع المبالغ المستحقة لجميع الأعضاء.</p>
        <Button onClick={onReset} variant="outline">
          بدء دورة جديدة
        </Button>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 pb-24">
      {/* Android Header */}
      <div className="bg-primary text-white p-6 pb-8 rounded-b-[2rem] shadow-lg sticky top-0 z-10">
        <div className="flex justify-between items-start mb-6">
          <div>
            <h1 className="text-xl font-bold">{data.name}</h1>
            <div className="flex items-center gap-2 opacity-90 text-sm mt-1 bg-white/10 px-2 py-1 rounded-lg w-fit">
              <Calendar size={14} />
              <span>{getMonthName(data.startDate, currentIndex)}</span>
            </div>
          </div>
          <button onClick={onReset} className="p-2 bg-white/10 rounded-full hover:bg-white/20 active:bg-white/30 transition-colors">
            <RefreshCw size={18} />
          </button>
        </div>

        <div className="flex gap-4">
          <div className="flex-1">
             <div className="text-emerald-100 text-xs mb-1">تم تحصيل</div>
             <div className="text-3xl font-bold font-mono">{formatCurrency(totalCollected)}</div>
          </div>
          <div className="flex-1 text-right">
             <div className="text-emerald-100 text-xs mb-1">المتبقي</div>
             <div className="text-2xl font-bold font-mono opacity-90">{formatCurrency(remaining)}</div>
          </div>
        </div>
      </div>

      {/* Main Content */}
      <div className="px-4 -mt-6 relative z-20">
        
        {/* Recipient Card */}
        <div className="bg-white rounded-2xl p-4 shadow-md mb-4 border border-gray-100">
            <div className="flex justify-between items-center mb-3">
              <span className="text-xs text-gray-500 font-bold uppercase">عليه الدور (يقبض)</span>
              {currentHistory.payoutCompleted && <span className="text-xs bg-green-100 text-green-700 px-2 py-0.5 rounded-full font-bold">تم الاستلام</span>}
            </div>
            
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-full bg-yellow-100 flex items-center justify-center text-yellow-700 font-bold border border-yellow-200">
                  {recipientMember.name.charAt(0)}
                </div>
                <p className="text-xl font-bold text-gray-800">{recipientMember.name}</p>
              </div>
              
              <button 
                onClick={handlePayoutToggle}
                disabled={!isCollectionComplete}
                className={`w-12 h-12 rounded-full flex items-center justify-center transition-all shadow-sm ${
                   currentHistory.payoutCompleted ? 'bg-green-500 text-white' : 
                   isCollectionComplete ? 'bg-gray-100 text-gray-400 hover:bg-green-500 hover:text-white' : 'bg-gray-50 text-gray-300'
                }`}
              >
                 <Wallet size={24} />
              </button>
            </div>
        </div>

        {/* Members List Header */}
        <div className="flex items-center justify-between px-2 mb-2">
           <h3 className="font-bold text-gray-700">قائمة المشتركين</h3>
           <span className="text-xs text-gray-400">{paidCount}/{data.members.length} دفعوا</span>
        </div>

        {/* Member List */}
        <div className="space-y-3">
          {data.members.map((member) => {
            const isPaid = !!currentHistory.payments[member.id];
            const payoutMonthName = getMonthName(data.startDate, member.order);
            const isRecipient = member.id === recipientMember.id;

            return (
              <div 
                key={member.id}
                onClick={() => togglePayment(member.id)}
                className={`
                  flex items-center justify-between p-4 rounded-xl border transition-all active:scale-[0.99]
                  ${isPaid 
                    ? 'bg-white border-emerald-100' 
                    : 'bg-white border-gray-200'
                  }
                  ${isRecipient ? 'border-l-4 border-l-yellow-400' : ''}
                `}
              >
                <div className="flex items-center gap-4">
                  <div className={`
                    w-10 h-10 rounded-full flex items-center justify-center text-sm font-bold transition-colors
                    ${isPaid ? 'bg-emerald-100 text-emerald-600' : 'bg-gray-100 text-gray-400'}
                  `}>
                    {isPaid ? <CheckCircle size={20} /> : member.order + 1}
                  </div>
                  <div>
                    <h3 className={`font-bold ${isPaid ? 'text-gray-800' : 'text-gray-600'}`}>
                      {member.name}
                    </h3>
                    <p className="text-[10px] text-gray-400">
                      موعده: {payoutMonthName}
                    </p>
                  </div>
                </div>

                <div className={`
                  w-6 h-6 rounded-full border-2 flex items-center justify-center transition-colors
                  ${isPaid 
                    ? 'bg-emerald-500 border-emerald-500' 
                    : 'border-gray-200 bg-gray-50'
                  }
                `}>
                   {isPaid && <CheckCircle size={14} className="text-white" />}
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Floating Action Button for Next Month */}
      {currentHistory.payoutCompleted && (
        <div className="fixed bottom-6 left-0 right-0 px-6 z-50">
           <Button 
             onClick={moveToNextMonth} 
             fullWidth 
             className="shadow-xl shadow-emerald-200 bg-gray-900 hover:bg-black text-white rounded-full py-4"
           >
             <span className="text-lg">الشهر التالي</span>
             <ArrowRight size={20} className="mr-1" />
           </Button>
        </div>
      )}
      
      {/* Install Hint (Only visible if not pwa mode roughly check) */}
      <div className="text-center mt-8 text-gray-400 text-xs flex items-center justify-center gap-1">
         <Smartphone size={12} />
         <span>أضف التطبيق للشاشة الرئيسية لسهولة الوصول</span>
      </div>
    </div>
  );
};

export default App;